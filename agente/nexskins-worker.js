// NexSkins: servidor de skins de NexLauncher para Cloudflare Workers (plan gratis) + Workers KV.
// Habla el formato CustomSkinAPI del mod CustomSkinLoader:
//   GET  /<nombre>.json      -> {"username", "skins": {"default"|"slim": hash}, "cape": hash}
//   GET  /textures/<hash>    -> la imagen PNG
//   POST /subir              -> NexLauncher sube la skin offline de una cuenta (JSON)
//   GET  /indice             -> {"nombres": [...]}: quién tiene skin (NexLauncher solo pregunta por esos)
// Un nombre es de quien lo registró primero: las siguientes subidas necesitan el mismo token.
// En el panel de Cloudflare el Worker necesita un "KV namespace binding" llamado SKINS.

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
        m = ruta.match(/^\/(\w{1,16})\.json$/);
        if (m) return await perfil(env, m[1]);
        return json({ error: "no existe" }, 404);
      }
      if (req.method === "POST" && ruta === "/subir") return await subir(req, env);
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
  return json(r, 200, { "Cache-Control": "public, max-age=60" });
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
