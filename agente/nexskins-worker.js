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

const MAX_PNG = 64 * 1024;
const NOMBRE = /^\w{1,16}$/;

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    const ruta = decodeURIComponent(url.pathname);
    try {
      if (req.method === "GET" || req.method === "HEAD") {
        if (ruta === "/" ) return json({ servicio: "NexSkins", formato: "CustomSkinAPI" });
        if (ruta === "/indice") {
          const nombres = (await env.SKINS.get("indice", "json")) || [];
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
      return json({ error: "interno" }, 500);
    }
  },
};

function json(obj, estado = 200, extra = {}) {
  return new Response(JSON.stringify(obj), {
    status: estado,
    headers: { "Content-Type": "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", ...extra },
  });
}

async function perfil(env, nombre) {
  const d = await env.SKINS.get("u:" + nombre.toLowerCase(), "json");
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
  const previo = await env.SKINS.get(clave, "json");
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
  const indice = (await env.SKINS.get("indice", "json")) || [];
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
