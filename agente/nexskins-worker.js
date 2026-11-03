// NexSkins: servidor de skins de NexLauncher para Cloudflare Workers (plan gratis) + Workers KV.
// Habla el formato CustomSkinAPI del mod CustomSkinLoader:
//   GET  /<nombre>.json      -> {"username", "skins": {"default"|"slim": hash}, "cape": hash}
//   GET  /textures/<hash>    -> la imagen PNG
//   POST /subir              -> NexLauncher sube la skin offline de una cuenta (JSON)
//   GET  /indice             -> {"nombres": [...]}: quién tiene skin (NexLauncher solo pregunta por esos)
// Un nombre es de quien lo registró primero: las siguientes subidas necesitan el mismo token.
// En el panel de Cloudflare el Worker necesita un "KV namespace binding" llamado SKINS.
//
// Cuentas de NexLauncher y amigos (base de datos D1, binding "DB"; las tablas se crean solas):
//   POST /cuenta/registro {usuario, clave}  POST /cuenta/entrar {usuario, clave}  -> {usuario, token}
//   POST /cuenta/salir    GET /cuenta/yo
//   GET  /amigos  -> {amigos: [{usuario, estado, visto, en_linea}], recibidas: [...], enviadas: [...]}
//   POST /amigos/solicitar {usuario}  POST /amigos/responder {usuario, aceptar}  POST /amigos/quitar {usuario}
//   POST /presencia {estado}
// «clave» llega ya derivada por NexLauncher (PBKDF2, 200 000 vueltas): aquí solo se guarda su SHA-256.
//
// Tendencias de la semana (D1): qué jugadores y capas se buscan y se ponen más en NexLauncher.
//   GET  /tendencias              -> {semana, jugadores: [{nombre, n}], capas: [{hash, nombre, n}]}
//   POST /tendencias {tipo: "jugador"|"capa", nombre, hash?}: cada PC cuenta una vez por semana y por skin/capa
//
// Cosméticos de los perfiles PvP (D1). Los pide NexLauncher, no el juego: el token de la cuenta no sale del launcher.
//   POST /cosmeticos/presencia {nombre, servidor}   (con Bearer: ligado a tu cuenta de NexLauncher)
//   POST /cosmeticos/lote {servidor, nombres: [...]} -> {jugadores: {nombre: {etiqueta, equipado}}}
//   GET  /cosmeticos/yo                              -> {usuario, equipado, etiqueta}       (Bearer)
//   POST /cosmeticos/equipar {categoria, id}         (id null = quitar)                     (Bearer)
//   POST /cosmeticos/capa   (cuerpo: el .gif)        -> {hash}: tu capa GIF                  (Bearer)
//   GET  /cosmeticos/gif/<hash>                      -> el .gif
//   POST /cosmeticos/admin/etiqueta {cuenta, etiqueta} con la cabecera X-Admin = secreto ADMIN_COSMETICOS
//
// CurseForge: /cf/v1/... reenvía a api.curseforge.com con la clave de la API guardada como secreto
// CF_API_KEY (Settings → Variables and Secrets), así la clave no va dentro del launcher. Solo deja pasar
// lo que usa la sección de mods (buscar, ver proyectos y bajar archivos).

const MAX_PNG = 64 * 1024;
const NOMBRE = /^\w{1,16}$/;

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    const ruta = decodeURIComponent(url.pathname);
    if (ruta.startsWith("/cf/")) return await curseforge(req, env, url, ruta.slice(3));
    if (ruta.startsWith("/cosmeticos/")) {
      if (!env.DB) return json({ error: "Falta la base de datos D1 (binding DB) en el Worker." }, 503);
      try {
        await esquema(env);
        await esquemaCosmeticos(env);
        return await cosmeticos(req, env, ruta);
      } catch (e) {
        return json({ error: "interno", detalle: String((e && e.message) || e).slice(0, 300) }, 500);
      }
    }
    if (ruta === "/tendencias") {
      if (!env.DB) return json({ error: "Falta la base de datos D1 (binding DB) en el Worker." }, 503);
      try {
        await esquemaTendencias(env);
        return await tendencias(req, env);
      } catch (e) {
        return json({ error: "interno", detalle: String((e && e.message) || e).slice(0, 300) }, 500);
      }
    }
    const deCuentas = ruta.startsWith("/cuenta/") || ruta === "/amigos" || ruta.startsWith("/amigos/") || ruta === "/presencia";
    if (!env.SKINS && !deCuentas && ruta !== "/") {
      return json({ error: "Falta el KV de skins (binding SKINS) en el Worker." }, 503);
    }
    try {
      if (req.method === "GET" || req.method === "HEAD") {
        if (ruta === "/" ) return json({ servicio: "NexSkins", formato: "CustomSkinAPI", skins: !!env.SKINS,
          skins_es_kv: esKv(env), cuentas: !!env.DB });
        if (ruta === "/indice") {
          const nombres = (await kvJson(env, "indice")) || [];
          return json({ nombres }, 200, { "Cache-Control": "public, max-age=120" });
        }
        let m = ruta.match(/^\/textures\/([0-9a-f]{64})$/);
        if (m) return await textura(env, m[1]);
        if (ruta === "/cuenta/yo" || ruta === "/amigos") {
          if (!env.DB) return json({ error: "Falta la base de datos D1 (binding DB) en el Worker." }, 503);
          await esquema(env);
          return await cuentas(req, env, ruta);
        }
        m = ruta.match(/^\/(\w{1,16})\.json$/);
        if (m) return await perfil(env, m[1]);
        return json({ error: "no existe" }, 404);
      }
      if (req.method === "POST" && ruta === "/subir") return await subir(req, env);
      if (ruta.startsWith("/cuenta/") || ruta === "/amigos" || ruta.startsWith("/amigos/") || ruta === "/presencia") {
        if (!env.DB) return json({ error: "Falta la base de datos D1 (binding DB) en el Worker." }, 503);
        await esquema(env);
        return await cuentas(req, env, ruta);
      }
      return json({ error: "método no permitido" }, 405);
    } catch (e) {
      return json({ error: "interno", detalle: String((e && e.message) || e).slice(0, 300) }, 500);
    }
  },
};

/** ¿SKINS es de verdad un KV namespace? (un R2 o una D1 con ese nombre también existen, pero no sirven) */
function esKv(env) {
  return !!env.SKINS && typeof env.SKINS.get === "function" && typeof env.SKINS.getWithMetadata === "function";
}

/** Lee un JSON del KV; si el valor está dañado devuelve null en vez de romper todo con un error 500. */
async function kvJson(env, clave) {
  if (!esKv(env)) {
    throw new Error("El binding SKINS no es un KV namespace (¿lo enlazaste a un R2 o a una D1?). En Settings → Bindings, SKINS debe ser «KV namespace».");
  }
  const texto = await env.SKINS.get(clave, "text");
  if (texto == null) return null;
  try { return JSON.parse(texto); } catch { return null; }
}

function json(obj, estado = 200, extra = {}) {
  return new Response(JSON.stringify(obj), {
    status: estado,
    headers: { "Content-Type": "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", ...extra },
  });
}

async function perfil(env, nombre) {
  const d = await kvJson(env, "u:" + nombre.toLowerCase());
  if (!d || (!d.skin && !d.capa)) return json({ error: "no existe" }, 404, { "Cache-Control": "public, max-age=60" });
  const r = { username: d.nombre, skins: {} };
  if (d.skin) r.skins[d.slim ? "slim" : "default"] = d.skin;
  if (d.capa) r.cape = d.capa;
  return json(r, 200, { "Cache-Control": "public, max-age=20" });
}

async function textura(env, hash) {
  const png = await env.SKINS.get("t:" + hash, "arrayBuffer");
  if (!png) return json({ error: "no existe" }, 404);
  return new Response(png, {
    headers: { "Content-Type": "image/png", "Cache-Control": "public, max-age=31536000, immutable",
               "Access-Control-Allow-Origin": "*" },
  });
}

async function sha256(datos) {
  const h = await crypto.subtle.digest("SHA-256", typeof datos === "string" ? new TextEncoder().encode(datos) : datos);
  return [...new Uint8Array(h)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function desdeBase64(b64) {
  const bin = atob(b64);
  const u = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i);
  return u;
}

// Revisa que sea un PNG y devuelve [ancho, alto]
function medidasPng(u) {
  const firma = [137, 80, 78, 71, 13, 10, 26, 10];
  if (u.length < 24 || firma.some((b, i) => u[i] !== b)) return null;
  const v = new DataView(u.buffer, u.byteOffset, u.byteLength);
  return [v.getUint32(16), v.getUint32(20)];
}

async function guardarPng(env, b64, esSkin) {
  if (!b64) return null;
  const u = desdeBase64(b64);
  if (u.length > MAX_PNG) throw new Error("imagen demasiado grande");
  const m = medidasPng(u);
  if (!m) throw new Error("no es PNG");
  const [w, h] = m;
  const ok = esSkin ? (w === 64 && (h === 64 || h === 32)) : (w >= 22 && w <= 1024 && h >= 17 && h <= 512 && w === 2 * h);
  if (!ok) throw new Error(esSkin ? "la skin debe ser de 64x64 o 64x32" : "medidas de capa inválidas");
  const hash = await sha256(u);
  if (!(await env.SKINS.get("t:" + hash, "arrayBuffer"))) await env.SKINS.put("t:" + hash, u);
  return hash;
}

async function subir(req, env) {
  const largo = Number(req.headers.get("Content-Length") || 0);
  if (largo > 3 * MAX_PNG) return json({ error: "demasiado grande" }, 413);
  let b;
  try { b = await req.json(); } catch { return json({ error: "JSON inválido" }, 400); }
  const nombre = String(b.name || "");
  const token = String(b.token || "");
  if (!NOMBRE.test(nombre)) return json({ error: "nombre inválido" }, 400);
  if (token.length < 32 || token.length > 128) return json({ error: "token inválido" }, 400);
  const clave = "u:" + nombre.toLowerCase();
  const previo = await kvJson(env, clave);
  const th = await sha256("nexskins:" + token);
  if (previo && previo.token !== th) return json({ error: "ese nombre ya es de otra persona" }, 403);
  let skin, capa;
  try {
    skin = await guardarPng(env, b.skin, true);
    capa = await guardarPng(env, b.capa, false);
  } catch (e) {
    return json({ error: e.message }, 400);
  }
  const nuevo = { nombre, token: th, skin, capa, slim: b.modelo === "slim", t: Date.now() };
  if (!previo || previo.skin !== skin || previo.capa !== capa || previo.slim !== nuevo.slim || previo.nombre !== nombre) {
    await env.SKINS.put(clave, JSON.stringify(nuevo));
  }
  const indice = (await kvJson(env, "indice")) || [];
  const n = nombre.toLowerCase();
  if (!indice.includes(n)) {
    indice.push(n);
    await env.SKINS.put("indice", JSON.stringify(indice));
  }
  return json({ ok: true });
}

// ---------------------------------------------------------------------------------------------------------
// Cuentas y amigos
let esquemaListo = false;
const USUARIO = /^[A-Za-z0-9_]{3,16}$/;
const EN_LINEA = 150; // segundos sin señal para contar como desconectado

async function esquema(env) {
  if (esquemaListo) return;
  await env.DB.batch([
    env.DB.prepare("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE COLLATE NOCASE, clave TEXT NOT NULL, creado INTEGER NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS sesiones (token TEXT PRIMARY KEY, usuario INTEGER NOT NULL, vence INTEGER NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS amistades (de INTEGER NOT NULL, para INTEGER NOT NULL, aceptada INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (de, para))"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS presencia (usuario INTEGER PRIMARY KEY, estado TEXT NOT NULL, visto INTEGER NOT NULL)"),
    env.DB.prepare("CREATE INDEX IF NOT EXISTS amistades_para ON amistades (para)"),
  ]);
  esquemaListo = true;
}

const ahora = () => Math.floor(Date.now() / 1000);

function iguales(a, b) {
  if (a.length !== b.length) return false;
  let d = 0;
  for (let i = 0; i < a.length; i++) d |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return d === 0;
}

async function cuerpo(req) {
  if (Number(req.headers.get("Content-Length") || 0) > 4096) throw new Error("demasiado grande");
  return await req.json();
}

async function nuevaSesion(env, usuario) {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  const token = [...bytes].map((b) => b.toString(16).padStart(2, "0")).join("");
  await env.DB.batch([
    env.DB.prepare("DELETE FROM sesiones WHERE vence < ?").bind(ahora()),
    env.DB.prepare("INSERT INTO sesiones (token, usuario, vence) VALUES (?, ?, ?)").bind(await sha256("sesion:" + token), usuario.id, ahora() + 90 * 86400),
  ]);
  return json({ usuario: usuario.nombre, token });
}

async function quien(req, env) {
  const m = (req.headers.get("Authorization") || "").match(/^Bearer ([0-9a-f]{64})$/);
  if (!m) return null;
  return await env.DB.prepare("SELECT u.id, u.nombre FROM sesiones s JOIN usuarios u ON u.id = s.usuario WHERE s.token = ? AND s.vence > ?")
    .bind(await sha256("sesion:" + m[1]), ahora()).first();
}

async function buscar(env, nombre) {
  if (!USUARIO.test(nombre || "")) return null;
  return await env.DB.prepare("SELECT id, nombre FROM usuarios WHERE nombre = ?").bind(nombre).first();
}

async function cuentas(req, env, ruta) {
  let b = {};
  if (req.method === "POST") {
    try { b = await cuerpo(req); } catch { return json({ error: "JSON inválido" }, 400); }
  }
  if (ruta === "/cuenta/registro" || ruta === "/cuenta/entrar") {
    const nombre = String(b.usuario || ""), clave = String(b.clave || "");
    if (!USUARIO.test(nombre)) return json({ error: "El usuario debe tener de 3 a 16 letras, números o _." }, 400);
    if (!/^[0-9a-f]{64}$/.test(clave)) return json({ error: "Contraseña inválida." }, 400);
    const guardada = await sha256("nexcuenta:" + clave);
    if (ruta === "/cuenta/registro") {
      try {
        await env.DB.prepare("INSERT INTO usuarios (nombre, clave, creado) VALUES (?, ?, ?)").bind(nombre, guardada, ahora()).run();
      } catch {
        return json({ error: "Ese usuario ya existe." }, 409);
      }
    }
    const u = await env.DB.prepare("SELECT id, nombre, clave FROM usuarios WHERE nombre = ?").bind(nombre).first();
    if (!u || !iguales(u.clave, guardada)) return json({ error: "Usuario o contraseña incorrectos." }, 401);
    return await nuevaSesion(env, u);
  }
  const yo = await quien(req, env);
  if (!yo) return json({ error: "Tu sesión de NexLauncher expiró. Vuelve a entrar." }, 401);
  if (ruta === "/cuenta/yo") return json({ usuario: yo.nombre });
  if (ruta === "/cuenta/salir") {
    const m = (req.headers.get("Authorization") || "").match(/^Bearer ([0-9a-f]{64})$/);
    await env.DB.batch([
      env.DB.prepare("DELETE FROM sesiones WHERE token = ?").bind(await sha256("sesion:" + m[1])),
      env.DB.prepare("UPDATE presencia SET visto = ? WHERE usuario = ?").bind(ahora() - EN_LINEA - 1, yo.id),
    ]);
    return json({ ok: true });
  }
  if (ruta === "/presencia") {
    const estado = JSON.stringify(b.estado || {});
    if (estado.length > 1000) return json({ error: "estado demasiado grande" }, 400);
    const visto = b.estado && b.estado.estado === "desconectado" ? ahora() - EN_LINEA - 1 : ahora();
    await env.DB.prepare("INSERT INTO presencia (usuario, estado, visto) VALUES (?, ?, ?) ON CONFLICT(usuario) DO UPDATE SET estado = excluded.estado, visto = excluded.visto")
      .bind(yo.id, estado, visto).run();
    return json({ ok: true });
  }
  if (ruta === "/amigos") {
    const [amigos, recibidas, enviadas] = await env.DB.batch([
      env.DB.prepare("SELECT u.nombre AS usuario, p.estado, p.visto FROM amistades a JOIN usuarios u ON u.id = CASE WHEN a.de = ?1 THEN a.para ELSE a.de END LEFT JOIN presencia p ON p.usuario = u.id WHERE a.aceptada = 1 AND (a.de = ?1 OR a.para = ?1) ORDER BY u.nombre").bind(yo.id),
      env.DB.prepare("SELECT u.nombre AS usuario FROM amistades a JOIN usuarios u ON u.id = a.de WHERE a.para = ? AND a.aceptada = 0").bind(yo.id),
      env.DB.prepare("SELECT u.nombre AS usuario FROM amistades a JOIN usuarios u ON u.id = a.para WHERE a.de = ? AND a.aceptada = 0").bind(yo.id),
    ]);
    const t = ahora();
    return json({
      usuario: yo.nombre,
      amigos: amigos.results.map((f) => {
        const en_linea = !!f.visto && t - f.visto < EN_LINEA;
        let estado = null;
        try { estado = en_linea && f.estado ? JSON.parse(f.estado) : null; } catch { estado = null; }
        return { usuario: f.usuario, en_linea, visto: f.visto || 0, estado };
      }),
      recibidas: recibidas.results.map((f) => f.usuario),
      enviadas: enviadas.results.map((f) => f.usuario),
    });
  }
  const otro = await buscar(env, String(b.usuario || ""));
  if (!otro) return json({ error: "No existe ningún usuario con ese nombre." }, 404);
  if (otro.id === yo.id) return json({ error: "No puedes agregarte a ti mismo." }, 400);
  if (ruta === "/amigos/solicitar") {
    const ya = await env.DB.prepare("SELECT de, aceptada FROM amistades WHERE (de = ?1 AND para = ?2) OR (de = ?2 AND para = ?1)").bind(yo.id, otro.id).first();
    if (ya && ya.aceptada) return json({ error: `${otro.nombre} ya es tu amigo.` }, 409);
    if (ya && ya.de === otro.id) {  // ya te lo había pedido: se aceptan los dos
      await env.DB.prepare("UPDATE amistades SET aceptada = 1 WHERE de = ? AND para = ?").bind(otro.id, yo.id).run();
      return json({ ok: true, aceptada: true });
    }
    if (ya) return json({ error: `Ya le enviaste una solicitud a ${otro.nombre}.` }, 409);
    const n = await env.DB.prepare("SELECT COUNT(*) AS n FROM amistades WHERE de = ? AND aceptada = 0").bind(yo.id).first();
    if (n && n.n >= 50) return json({ error: "Tienes demasiadas solicitudes sin responder." }, 429);
    await env.DB.prepare("INSERT INTO amistades (de, para, aceptada) VALUES (?, ?, 0)").bind(yo.id, otro.id).run();
    return json({ ok: true, aceptada: false });
  }
  if (ruta === "/amigos/responder") {
    const q = b.aceptar ? "UPDATE amistades SET aceptada = 1 WHERE de = ? AND para = ? AND aceptada = 0"
                        : "DELETE FROM amistades WHERE de = ? AND para = ? AND aceptada = 0";
    await env.DB.prepare(q).bind(otro.id, yo.id).run();
    return json({ ok: true });
  }
  if (ruta === "/amigos/quitar") {
    await env.DB.prepare("DELETE FROM amistades WHERE (de = ?1 AND para = ?2) OR (de = ?2 AND para = ?1)").bind(yo.id, otro.id).run();
    return json({ ok: true });
  }
  return json({ error: "no existe" }, 404);
}


// ---- CurseForge (proxy con la clave del Worker)
const CF_PERMITIDO = [
  /^\/v1\/mods\/search$/,            // GET  buscar
  /^\/v1\/mods\/\d+$/,               // GET  un proyecto
  /^\/v1\/mods\/\d+\/files$/,        // GET  archivos de un proyecto
  /^\/v1\/mods\/\d+\/files\/\d+$/,   // GET  un archivo
  /^\/v1\/mods$/,                    // POST varios proyectos {modIds}
  /^\/v1\/mods\/files$/,              // POST varios archivos {fileIds}
];

async function curseforge(req, env, url, ruta) {
  if (!env.CF_API_KEY) return json({ error: "Falta el secreto CF_API_KEY en el Worker." }, 503);
  if (!CF_PERMITIDO.some((r) => r.test(ruta))) return json({ error: "ruta no permitida" }, 404);
  const metodo = req.method;
  if (metodo !== "GET" && metodo !== "POST") return json({ error: "método no permitido" }, 405);
  if (metodo === "POST" && !/^\/v1\/mods(\/files)?$/.test(ruta)) return json({ error: "método no permitido" }, 405);
  const destino = "https://api.curseforge.com" + ruta + url.search;
  const cache = caches.default;
  const clave = new Request(destino, { method: "GET" });
  if (metodo === "GET") {  // las búsquedas se repiten mucho: 10 minutos en la caché de Cloudflare
    const guardada = await cache.match(clave);
    if (guardada) return guardada;
  }
  const cuerpo = metodo === "POST" ? await req.text() : undefined;
  if (cuerpo && cuerpo.length > 64 * 1024) return json({ error: "demasiado grande" }, 413);
  const r = await fetch(destino, {
    method: metodo,
    headers: { "x-api-key": env.CF_API_KEY, "Accept": "application/json", "Content-Type": "application/json" },
    body: cuerpo,
  });
  const resp = new Response(r.body, { status: r.status, headers: {
    "Content-Type": "application/json; charset=utf-8", "Cache-Control": "public, max-age=600" } });
  if (metodo === "GET" && r.ok) await cache.put(clave, resp.clone());
  return resp;
}


// ---- Tendencias de la semana
let tendenciasListas = false;
const HASH_TEX = /^[0-9a-f]{20,80}$/;
const semanaActual = () => Math.floor((Date.now() / 86400000 + 3) / 7);  // semanas que empiezan en lunes

async function esquemaTendencias(env) {
  if (tendenciasListas) return;
  await env.DB.batch([
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tend (semana INTEGER NOT NULL, tipo TEXT NOT NULL, clave TEXT NOT NULL, nombre TEXT NOT NULL, n INTEGER NOT NULL, PRIMARY KEY (semana, tipo, clave))"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tend_votos (semana INTEGER NOT NULL, tipo TEXT NOT NULL, clave TEXT NOT NULL, quien TEXT NOT NULL, PRIMARY KEY (semana, tipo, clave, quien))"),
    env.DB.prepare("CREATE INDEX IF NOT EXISTS tend_top ON tend (semana, tipo, n)"),
  ]);
  tendenciasListas = true;
}

async function tendencias(req, env) {
  const semana = semanaActual();
  if (req.method === "GET") {
    const top = async (tipo, s) => (await env.DB.prepare("SELECT clave, nombre, n FROM tend WHERE semana = ? AND tipo = ? ORDER BY n DESC LIMIT 48").bind(s, tipo).all()).results;
    const unir = async (tipo) => {
      const esta = await top(tipo, semana);
      if (esta.length >= 24) return esta;
      const vistas = new Set(esta.map((x) => x.clave));  // semana nueva: se completa con la anterior
      return esta.concat((await top(tipo, semana - 1)).filter((x) => !vistas.has(x.clave)).map((x) => ({ ...x, n: 0 })));
    };
    const [jug, cap] = [await unir("jugador"), await unir("capa")];
    return json({ semana, jugadores: jug.map((x) => ({ nombre: x.nombre, n: x.n })),
                  capas: cap.map((x) => ({ hash: x.clave, nombre: x.nombre, n: x.n })) }, 200, { "Cache-Control": "public, max-age=300" });
  }
  if (req.method !== "POST") return json({ error: "método no permitido" }, 405);
  let b;
  try { b = await cuerpo(req); } catch { return json({ error: "JSON inválido" }, 400); }
  const tipo = b.tipo === "capa" ? "capa" : b.tipo === "jugador" ? "jugador" : null;
  const nombre = String(b.nombre || "");
  if (!tipo || !NOMBRE.test(nombre)) return json({ error: "datos inválidos" }, 400);
  const clave = tipo === "capa" ? String(b.hash || "").toLowerCase() : nombre.toLowerCase();
  if (tipo === "capa" && !HASH_TEX.test(clave)) return json({ error: "datos inválidos" }, 400);
  const quien = await sha256("tend:" + (req.headers.get("CF-Connecting-IP") || "?") + ":" + semana);
  const voto = await env.DB.prepare("INSERT OR IGNORE INTO tend_votos (semana, tipo, clave, quien) VALUES (?, ?, ?, ?)")
    .bind(semana, tipo, clave, quien).run();
  if (voto.meta.changes) {
    const pasos = [env.DB.prepare("INSERT INTO tend (semana, tipo, clave, nombre, n) VALUES (?, ?, ?, ?, 1) ON CONFLICT(semana, tipo, clave) DO UPDATE SET n = n + 1, nombre = excluded.nombre")
      .bind(semana, tipo, clave, nombre)];
    if (Math.random() < 0.02) {  // limpieza: solo hacen falta esta semana y la anterior
      pasos.push(env.DB.prepare("DELETE FROM tend_votos WHERE semana < ?").bind(semana - 1));
      pasos.push(env.DB.prepare("DELETE FROM tend WHERE semana < ?").bind(semana - 1));
    }
    await env.DB.batch(pasos);
  }
  return json({ ok: true });
}


// ---- Cosméticos de los perfiles PvP
let cosmeticosListos = false;
const PRESENCIA_VIVA = 180;  // segundos sin aviso para dejar de mostrar la etiqueta
const MAX_GIF = 1500000;     // D1 guarda hasta 2 MB por fila
const ETIQUETA_BASE = { text: "NEXLAUNCHER", color: "gray" };
// Etiquetas fijas por cuenta de NexLauncher (además de las que se pongan con /cosmeticos/admin/etiqueta)
const ETIQUETAS = {
  zyocr0w: ["", { text: "aa", italic: true, obfuscated: true, color: "green" },
            { text: "CREADOR NEXLAUNCHER", bold: true, color: "white" },
            { text: "aa", italic: true, obfuscated: true, color: "green" }],
};
// Lo que existe: uno de cada categoría a la vez. "gif" = la capa GIF que subió esa cuenta.
const CATALOGO = {
  cabeza: ["aureola_dorada", "aureola_arcoiris", "aureola_oscura", "corona", "cuernos", "orejas_gato", "orejas_zorro", "nube",
           "bandana_fuego", "bandana_fuego_azul", "bandana_ninja", "bandana_arcoiris", "bandana_nex", "bandana_pirata",
           "corona_cempasuchil", "sombrero_charro", "gorro_navideno", "cuernos_reno", "calabaza", "sombrero_bruja"],
  espalda: ["alas_angel", "alas_angel_oscuro", "alas_angel_arcoiris", "alas_dragon", "alas_dragon_negro", "alas_dragon_hielo",
            "alas_dragon_ender", "alas_mariposa", "alas_fuego", "jetpack"],
  cuerpo: ["cola_zorro", "cola_gato", "cola_dragon", "bufanda", "orbitas", "sarape", "bufanda_navidena"],
  capa: ["gif", "capa_brillo", "capa_color", "capa_real", "capa_estelar", "capa_nex", "capa_llamas", "capa_arcoiris",
         "capa_vampiro"],
  aura: ["ki_blanco", "ki_dorado", "ki_azul", "ki_rayos", "aura_oscura", "aura_fuego", "aura_arcoiris", "aura_hielo", "nieve",
         "aura_murcielagos"],
  rastro: ["chispas", "corazones", "notas", "pisadas_fuego"],
  escudo: ["escudo_nex", "escudo_noche", "escudo_dragon", "escudo_dorado", "escudo_hielo", "escudo_fuego", "escudo_obsidiana"],
  mascota: ["dragoncito", "gatito", "perrito", "buho", "fantasma", "axolote", "alebrije", "alebrije_noche", "aguila", "rodolfo",
            "murcielago"],
  mano: ["maracas", "jarrito", "baston_dulce", "calabacita"],
};
// De temporada: quien entra (abre sus cosméticos en NexLauncher o juega con su cuenta) durante la festividad se queda
// con los de esa festividad para siempre. Fuera de ella no se pueden conseguir. Los creadores tienen todos.
const TEMPORADA_COS = {
  alebrije: "muertos", alebrije_noche: "muertos", corona_cempasuchil: "muertos",
  aguila: "patrias", sombrero_charro: "patrias", sarape: "patrias", maracas: "patrias", jarrito: "patrias",
  rodolfo: "navidad", gorro_navideno: "navidad", cuernos_reno: "navidad", bufanda_navidena: "navidad",
  baston_dulce: "navidad", nieve: "navidad",
  calabaza: "halloween", sombrero_bruja: "halloween", capa_vampiro: "halloween", murcielago: "halloween",
  calabacita: "halloween", aura_murcielagos: "halloween",
};
const CREADORES = ["zyocr0w"];

/** Festividad de una fecha: halloween (1–24 oct), muertos (25 oct – 3 nov), patrias (1–16 sep y 4–20 nov),
 *  navidad (1 dic – 6 ene). */
function festividad(f) {
  const m = f.getUTCMonth() + 1, d = f.getUTCDate();
  if (m === 10 && d <= 24) return "halloween";
  if ((m === 10 && d >= 25) || (m === 11 && d <= 3)) return "muertos";
  if ((m === 9 && d <= 16) || (m === 11 && d >= 4 && d <= 20)) return "patrias";
  if (m === 12 || (m === 1 && d <= 6)) return "navidad";
  return null;
}

/** Festividades que tocan ahora en algún lugar del mundo (de UTC−12 a UTC+14, para no dejar fuera a nadie). */
function festividadesAhora() {
  const ms = Date.now();
  return [...new Set([-12, -6, 0, 14].map((h) => festividad(new Date(ms + h * 3600000))).filter(Boolean))];
}

const esCreador = (u) => !!u && CREADORES.includes(String(u.nombre).toLowerCase());

/**
 * Cosméticos de temporada que ya son de esta cuenta. Si ahora es temporada, primero se le dan los de esa fiesta
 * (se guardan para siempre). Los creadores tienen todos.
 */
async function desbloqueados(env, u) {
  if (esCreador(u)) return Object.keys(TEMPORADA_COS);
  const fila = await env.DB.prepare("SELECT ids FROM cos_desbloqueado WHERE usuario = ?").bind(u.id).first();
  let ids = [];
  try { ids = JSON.parse(fila?.ids || "[]").filter((i) => TEMPORADA_COS[i]); } catch { ids = []; }
  const fiestas = festividadesAhora();
  const nuevos = Object.keys(TEMPORADA_COS).filter((i) => fiestas.includes(TEMPORADA_COS[i]) && !ids.includes(i));
  if (nuevos.length) {
    ids = ids.concat(nuevos);
    await env.DB.prepare("INSERT INTO cos_desbloqueado (usuario, ids) VALUES (?, ?) ON CONFLICT(usuario) DO UPDATE SET ids = excluded.ids")
      .bind(u.id, JSON.stringify(ids)).run();
  }
  return ids;
}

async function esquemaCosmeticos(env) {
  if (cosmeticosListos) return;
  await env.DB.batch([
    env.DB.prepare("CREATE TABLE IF NOT EXISTS cos_presencia (nombre TEXT NOT NULL COLLATE NOCASE, servidor TEXT NOT NULL, cuenta INTEGER, visto INTEGER NOT NULL, PRIMARY KEY (nombre, servidor))"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS cos_equipado (usuario INTEGER PRIMARY KEY, datos TEXT NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS cos_desbloqueado (usuario INTEGER PRIMARY KEY, ids TEXT NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS cos_etiqueta (usuario INTEGER PRIMARY KEY, etiqueta TEXT NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS cos_gif (hash TEXT PRIMARY KEY, datos BLOB NOT NULL, usuario INTEGER NOT NULL, creado INTEGER NOT NULL)"),
  ]);
  // premium: la cuenta de Minecraft con la que juega (logo verde encima del nombre; offline, gris)
  try { await env.DB.prepare("ALTER TABLE cos_presencia ADD COLUMN premium INTEGER NOT NULL DEFAULT 0").run(); } catch { /* ya existe */ }
  cosmeticosListos = true;
}

/** "Play.Server.net:25565" -> "play.server.net" (el puerto por defecto no cuenta). */
function servidorNormal(s) {
  s = String(s || "").trim().toLowerCase().replace(/\.$/, "");
  if (s.endsWith(":25565")) s = s.slice(0, -6);
  return /^[a-z0-9.\-:\[\]_]{0,100}$/.test(s) ? s : "";
}

function leerEquipado(texto) {
  try {
    const d = JSON.parse(texto || "{}");
    const r = {};
    for (const [cat, ids] of Object.entries(CATALOGO)) {
      const v = d[cat];
      if (cat === "capa" && v && typeof v === "object" && v.id === "gif" && /^[0-9a-f]{64}$/.test(v.hash || "")) r.capa = { id: "gif", hash: v.hash };
      else if (typeof v === "string" && ids.includes(v) && v !== "gif") r[cat] = v;
    }
    return r;
  } catch {
    return {};
  }
}

async function etiquetaDe(env, usuario) {
  if (!usuario) return ETIQUETA_BASE;
  const fija = ETIQUETAS[usuario.nombre.toLowerCase()];
  if (fija) return fija;
  const r = await env.DB.prepare("SELECT etiqueta FROM cos_etiqueta WHERE usuario = ?").bind(usuario.id).first();
  if (r) {
    try { return JSON.parse(r.etiqueta); } catch { /* etiqueta dañada: la base */ }
  }
  return ETIQUETA_BASE;
}

/** Cuenta los cuadros de un GIF recorriendo sus bloques; null si no es un GIF válido. */
function cuadrosGif(b) {
  const tx = String.fromCharCode(...b.slice(0, 6));
  if ((tx !== "GIF87a" && tx !== "GIF89a") || b.length < 13) return null;
  let i = 13;
  if (b[10] & 0x80) i += 3 * (1 << ((b[10] & 7) + 1));
  let cuadros = 0;
  const subbloques = () => { while (i < b.length && b[i] !== 0) i += b[i] + 1; i++; };
  while (i < b.length) {
    const t = b[i++];
    if (t === 0x3b) return cuadros;            // fin
    if (t === 0x21) { i++; subbloques(); }     // extensión
    else if (t === 0x2c) {                     // imagen
      if (i + 9 > b.length) return null;
      const f = b[i + 8];
      i += 9;
      if (f & 0x80) i += 3 * (1 << ((f & 7) + 1));
      i++;                                     // tamaño mínimo LZW
      subbloques();
      cuadros++;
    } else return cuadros > 0 ? cuadros : null;
  }
  return cuadros > 0 ? cuadros : null;
}

async function cosmeticos(req, env, ruta) {
  const t = ahora();
  let m = ruta.match(/^\/cosmeticos\/gif\/([0-9a-f]{64})$/);
  if (m && req.method === "GET") {
    const r = await env.DB.prepare("SELECT datos FROM cos_gif WHERE hash = ?").bind(m[1]).first();
    if (!r) return json({ error: "no existe" }, 404);
    return new Response(new Uint8Array(r.datos), { headers: { "Content-Type": "image/gif", "Cache-Control": "public, max-age=31536000, immutable" } });
  }
  if (ruta === "/cosmeticos/admin/etiqueta" && req.method === "POST") {
    if (!env.ADMIN_COSMETICOS || !iguales(req.headers.get("X-Admin") || "", env.ADMIN_COSMETICOS)) return json({ error: "no autorizado" }, 401);
    let b;
    try { b = await cuerpo(req); } catch { return json({ error: "JSON inválido" }, 400); }
    const u = await buscar(env, b.cuenta);
    if (!u) return json({ error: "no existe esa cuenta" }, 404);
    if (b.etiqueta == null) await env.DB.prepare("DELETE FROM cos_etiqueta WHERE usuario = ?").bind(u.id).run();
    else await env.DB.prepare("INSERT INTO cos_etiqueta (usuario, etiqueta) VALUES (?, ?) ON CONFLICT(usuario) DO UPDATE SET etiqueta = excluded.etiqueta").bind(u.id, JSON.stringify(b.etiqueta).slice(0, 2000)).run();
    return json({ ok: true });
  }
  if (ruta === "/cosmeticos/lote" && req.method === "POST") {
    let b;
    try { b = await cuerpo(req); } catch { return json({ error: "JSON inválido" }, 400); }
    const srv = servidorNormal(b.servidor);
    const nombres = [...new Set((Array.isArray(b.nombres) ? b.nombres : []).map(String).filter((n) => NOMBRE.test(n)).map((n) => n.toLowerCase()))].slice(0, 100);
    const jugadores = {};
    if (!nombres.length) return json({ jugadores });
    const marcas = nombres.map(() => "?").join(",");
    const filas = (await env.DB.prepare(`SELECT p.nombre, p.cuenta, p.premium, u.nombre AS usuario, e.datos FROM cos_presencia p LEFT JOIN usuarios u ON u.id = p.cuenta LEFT JOIN cos_equipado e ON e.usuario = p.cuenta WHERE p.nombre IN (${marcas}) AND p.servidor = ? AND p.visto > ? ORDER BY p.visto`)
      .bind(...nombres, srv, t - PRESENCIA_VIVA).all()).results;
    for (const f of filas) {
      const usuario = f.cuenta && f.usuario ? { id: f.cuenta, nombre: f.usuario } : null;
      // insignia: el logo de NexLauncher encima del nombre (arcoíris el creador, verde premium, gris offline)
      const insignia = esCreador(usuario) ? "creador" : f.premium ? "premium" : "offline";
      jugadores[f.nombre.toLowerCase()] = { etiqueta: await etiquetaDe(env, usuario), insignia, equipado: usuario ? leerEquipado(f.datos) : {} };
    }
    return json({ jugadores });
  }
  // Lo que sigue va ligado (o puede ir ligado) a una cuenta de NexLauncher
  const yo = await quien(req, env);
  if (ruta === "/cosmeticos/presencia" && req.method === "POST") {
    let b;
    try { b = await cuerpo(req); } catch { return json({ error: "JSON inválido" }, 400); }
    const nombre = String(b.nombre || "");
    if (!NOMBRE.test(nombre)) return json({ error: "nombre inválido" }, 400);
    const pasos = [env.DB.prepare("INSERT INTO cos_presencia (nombre, servidor, cuenta, visto, premium) VALUES (?, ?, ?, ?, ?) ON CONFLICT(nombre, servidor) DO UPDATE SET cuenta = excluded.cuenta, visto = excluded.visto, premium = excluded.premium")
      .bind(nombre, servidorNormal(b.servidor), yo ? yo.id : null, t, b.premium ? 1 : 0)];
    if (Math.random() < 0.02) pasos.push(env.DB.prepare("DELETE FROM cos_presencia WHERE visto < ?").bind(t - 3600));
    await env.DB.batch(pasos);
    if (yo && festividadesAhora().length) await desbloqueados(env, yo);  // jugar en temporada también cuenta
    return json({ ok: true, cuenta: yo ? yo.nombre : null });
  }
  if (!yo) return json({ error: "Inicia sesión con tu cuenta de NexLauncher para usar cosméticos." }, 401);
  const actual = async () => leerEquipado((await env.DB.prepare("SELECT datos FROM cos_equipado WHERE usuario = ?").bind(yo.id).first())?.datos);
  const guardar = (eq) => env.DB.prepare("INSERT INTO cos_equipado (usuario, datos) VALUES (?, ?) ON CONFLICT(usuario) DO UPDATE SET datos = excluded.datos").bind(yo.id, JSON.stringify(eq)).run();
  if (ruta === "/cosmeticos/yo" && req.method === "GET") {
    return json({ usuario: yo.nombre, equipado: await actual(), etiqueta: await etiquetaDe(env, yo), catalogo: CATALOGO,
                  temporada: festividad(new Date(Date.now() - 6 * 3600000)), desbloqueados: await desbloqueados(env, yo),
                  creador: esCreador(yo) });
  }
  if (ruta === "/cosmeticos/equipar" && req.method === "POST") {
    let b;
    try { b = await cuerpo(req); } catch { return json({ error: "JSON inválido" }, 400); }
    const ids = CATALOGO[b.categoria];
    if (!ids) return json({ error: "categoría inválida" }, 400);
    const eq = await actual();
    if (b.id == null) delete eq[b.categoria];
    else if (b.categoria === "capa" && b.id === "gif") {
      const r = await env.DB.prepare("SELECT hash FROM cos_gif WHERE usuario = ? ORDER BY creado DESC LIMIT 1").bind(yo.id).first();
      if (!r) return json({ error: "Primero sube tu capa GIF." }, 400);
      eq.capa = { id: "gif", hash: r.hash };
    } else if (ids.includes(b.id)) {
      if (TEMPORADA_COS[b.id] && !(await desbloqueados(env, yo)).includes(b.id)) {
        return json({ error: "Ese cosmético es de temporada: se consigue entrando durante su festividad." }, 403);
      }
      eq[b.categoria] = b.id;
    } else return json({ error: "ese cosmético no existe" }, 400);
    await guardar(eq);
    return json({ equipado: eq });
  }
  if (ruta === "/cosmeticos/capa" && req.method === "POST") {
    if (Number(req.headers.get("Content-Length") || 0) > MAX_GIF) return json({ error: "El GIF pesa más de 1.5 MB." }, 413);
    const datos = new Uint8Array(await req.arrayBuffer());
    if (datos.length > MAX_GIF) return json({ error: "El GIF pesa más de 1.5 MB." }, 413);
    const n = cuadrosGif(datos);
    if (!n) return json({ error: "Ese archivo no es un GIF válido." }, 400);
    if (n > 256) return json({ error: "El GIF tiene demasiados cuadros (máximo 256)." }, 400);
    const hash = await sha256(datos);
    await env.DB.batch([
      env.DB.prepare("DELETE FROM cos_gif WHERE usuario = ? AND hash != ?").bind(yo.id, hash),  // una capa GIF por cuenta
      env.DB.prepare("INSERT OR IGNORE INTO cos_gif (hash, datos, usuario, creado) VALUES (?, ?, ?, ?)").bind(hash, datos, yo.id, t),
    ]);
    const eq = await actual();
    eq.capa = { id: "gif", hash };
    await guardar(eq);
    return json({ hash, cuadros: n, equipado: eq });
  }
  return json({ error: "no existe" }, 404);
}
