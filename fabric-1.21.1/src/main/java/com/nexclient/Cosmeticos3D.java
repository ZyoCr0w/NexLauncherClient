package com.nexclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intérprete de cosmeticos.json (el mismo archivo que usa la vista previa de NexLauncher, en Python): grupos de
 * cajas con giros animados y emisores de partículas. Devuelve cajas ya colocadas (8 esquinas en píxeles, en el
 * espacio de modelo de Minecraft: Y hacia abajo, origen en el cuello, frente hacia -Z). Mismo archivo en 1.8.9 y 1.21.1.
 */
public final class Cosmeticos3D {
    /** Una caja ya colocada: esquinas[i] con i = x + 2·y + 4·z; color en 0..1; brillo = se ve iluminada siempre. */
    public static final class Cubo {
        public final float[][] esquinas;
        public final float r, g, b, a;
        public final boolean brillo;

        Cubo(float[][] esquinas, float r, float g, float b, float a, boolean brillo) {
            this.esquinas = esquinas;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.brillo = brillo;
        }
    }

    /** Estado del jugador: t en segundos; mov, aire, golpe en 0..1; agachado 0 o 1. */
    public static final class Estado {
        public float t, mov, aire, agachado, golpe;
    }

    private static JsonObject defs;

    private Cosmeticos3D() {
    }

    private static JsonObject defs() {
        if (defs == null) {
            try (InputStream in = Cosmeticos3D.class.getResourceAsStream("/assets/nexclient/cosmeticos.json")) {
                defs = in == null ? new JsonObject()
                        : new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                System.err.println("[NexClient] cosmeticos.json ilegible: " + e);
                defs = new JsonObject();
            }
        }
        return defs;
    }

    /** Hueso al que va pegado: "cabeza", "cuerpo", "jugador" o "escudo" (null si no existe). */
    public static String hueso(String id) {
        JsonObject d = id == null ? null : obj(defs(), id);
        return d == null ? null : str(d, "hueso", "cuerpo");
    }

    // ---- matrices 4×4 por filas
    private static float[] identidad() {
        return new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static float[] mul(float[] a, float[] b) {
        float[] r = new float[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                r[i * 4 + j] = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j] + a[i * 4 + 3] * b[12 + j];
            }
        }
        return r;
    }

    private static float[] tras(float x, float y, float z) {
        return new float[]{1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1};
    }

    private static float[] rot(float rx, float ry, float rz) {
        double ax = Math.toRadians(rx), ay = Math.toRadians(ry), az = Math.toRadians(rz);
        float cx = (float) Math.cos(ax), sx = (float) Math.sin(ax), cy = (float) Math.cos(ay), sy = (float) Math.sin(ay);
        float cz = (float) Math.cos(az), sz = (float) Math.sin(az);
        float[] mx = {1, 0, 0, 0, 0, cx, -sx, 0, 0, sx, cx, 0, 0, 0, 0, 1};
        float[] my = {cy, 0, sy, 0, 0, 1, 0, 0, -sy, 0, cy, 0, 0, 0, 0, 1};
        float[] mz = {cz, -sz, 0, 0, sz, cz, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        return mul(mul(mz, my), mx);
    }

    private static final float[] ESPEJO = {-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    private static float[][] caja(float[] m, float x, float y, float z, float w, float h, float d) {
        float[][] p = new float[8][];
        int k = 0;
        for (int dz = 0; dz < 2; dz++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dx = 0; dx < 2; dx++) {
                    float px = x + dx * w, py = y + dy * h, pz = z + dz * d;
                    p[k++] = new float[]{m[0] * px + m[1] * py + m[2] * pz + m[3], m[4] * px + m[5] * py + m[6] * pz + m[7],
                            m[8] * px + m[9] * py + m[10] * pz + m[11]};
                }
            }
        }
        return p;
    }

    // ---- utilidades (iguales a las de Python)
    static float hash(long... v) {
        long h = 2166136261L;
        for (long x : v) {
            h = ((h ^ (x & 0xFFFFFFFFL)) * 16777619L) & 0xFFFFFFFFL;
        }
        h ^= h >>> 13;
        h = (h * 0x5bd1e995L) & 0xFFFFFFFFL;
        h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float[] color(String c) {
        c = c.startsWith("#") ? c.substring(1) : c;
        return new float[]{Integer.parseInt(c.substring(0, 2), 16) / 255f, Integer.parseInt(c.substring(2, 4), 16) / 255f,
                Integer.parseInt(c.substring(4, 6), 16) / 255f};
    }

    private static float[] hsv(float h, float s, float v) {
        int i = ((int) (h * 6)) % 6;
        float f = h * 6 - (int) (h * 6), p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        switch (i) {
            case 0: return new float[]{v, t, p};
            case 1: return new float[]{q, v, p};
            case 2: return new float[]{p, v, t};
            case 3: return new float[]{p, q, v};
            case 4: return new float[]{t, p, v};
            default: return new float[]{v, p, q};
        }
    }

    private static JsonObject obj(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static JsonArray arr(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }

    private static float num(JsonObject o, String k, float def) {
        JsonElement e = o == null ? null : o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsFloat() : def;
    }

    private static String str(JsonObject o, String k, String def) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    private static boolean bool(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() && e.getAsBoolean();
    }

    private static float[] vec(JsonObject o, String k, float a, float b, float c) {
        JsonArray v = arr(o, k);
        return v.size() >= 3 ? new float[]{v.get(0).getAsFloat(), v.get(1).getAsFloat(), v.get(2).getAsFloat()}
                : new float[]{a, b, c};
    }

    private static float[] par(JsonObject o, String k, float a, float b) {
        JsonArray v = arr(o, k);
        return v.size() >= 2 ? new float[]{v.get(0).getAsFloat(), v.get(1).getAsFloat()} : new float[]{a, b};
    }

    private static float valor(JsonObject a, Estado e, float base) {
        if (a == null) {
            return base;
        }
        // quieto y en movimiento son dos ondas de frecuencia fija que se mezclan según mov: cambiar la frecuencia
        // con mov haría saltar la fase (t es la hora en segundos) y el ala «aletearía» de golpe
        float amp = num(a, "amp", 0), f = num(a, "frec", 0), fase = num(a, "fase", 0);
        float ampMov = amp * (1 + num(a, "amp_mov", 0)), fMov = f + num(a, "frec_mov", 0);
        float v = base + num(a, "vel", 0) * e.t + num(a, "mov", 0) * e.mov + num(a, "aire", 0) * e.aire
                + num(a, "agachado", 0) * e.agachado + num(a, "golpe", 0) * e.golpe;
        if (amp != 0) {
            v += amp * (1 - e.mov) * (float) Math.sin(2 * Math.PI * f * e.t + fase)
                    + ampMov * e.mov * (float) Math.sin(2 * Math.PI * fMov * e.t + fase);
        }
        return v;
    }

    // ---- intérprete
    /** Las cajas del cosmético id en el estado e (lista vacía si no existe). */
    public static List<Cubo> cubos(String id, Estado e) {
        List<Cubo> salida = new ArrayList<Cubo>();
        JsonObject d = id == null ? null : obj(defs(), id);
        if (d == null) {
            return salida;
        }
        final Map<String, JsonObject> grupos = new LinkedHashMap<String, JsonObject>();
        JsonArray lista = arr(d, "grupos");
        for (int i = 0; i < lista.size(); i++) {
            JsonObject g = lista.get(i).getAsJsonObject();
            grupos.put(g.has("id") ? g.get("id").getAsString() : String.valueOf(i), g);
        }
        for (int lado = 1; lado >= -1; lado -= 2) {
            Map<String, float[]> matrices = new HashMap<String, float[]>();
            for (Map.Entry<String, JsonObject> en : grupos.entrySet()) {
                JsonObject g = en.getValue();
                if (lado < 0 && !bool(g, "espejo")) {
                    continue;
                }
                float[] m = matriz(en.getKey(), grupos, matrices, e, lado);
                if (lado < 0) {
                    m = mul(m, ESPEJO);
                }
                JsonArray piezas = arr(g, "piezas");
                for (int k = 0; k < piezas.size(); k++) {
                    pieza(piezas.get(k).getAsJsonObject(), m, e, k, salida);
                }
            }
        }
        JsonArray em = arr(d, "emisores");
        for (int k = 0; k < em.size(); k++) {
            emisor(em.get(k).getAsJsonObject(), e, k, salida);
        }
        return salida;
    }

    private static float[] matriz(String gid, Map<String, JsonObject> grupos, Map<String, float[]> hechas, Estado e, int lado) {
        float[] m = hechas.get(gid);
        if (m != null) {
            return m;
        }
        JsonObject g = grupos.get(gid);
        String padre = g.has("padre") && !g.get("padre").isJsonNull() ? g.get("padre").getAsString() : null;
        float[] base = padre != null && grupos.containsKey(padre) ? matriz(padre, grupos, hechas, e, lado) : identidad();
        float[] piv = vec(g, "pivote", 0, 0, 0), r = vec(g, "rot", 0, 0, 0);
        JsonObject an = obj(g, "anim");
        if (an == null) {
            an = new JsonObject();
        }
        float ox = valor(obj(an, "x"), e, 0), oy = valor(obj(an, "y"), e, 0), oz = valor(obj(an, "z"), e, 0);
        float rx = valor(obj(an, "rx"), e, r[0]), ry = valor(obj(an, "ry"), e, r[1]), rz = valor(obj(an, "rz"), e, r[2]);
        float px = piv[0];
        if (lado < 0) {  // copia en espejo
            px = -px;
            ox = -ox;
            ry = -ry;
            rz = -rz;
        }
        m = mul(base, mul(tras(px + ox, piv[1] + oy, piv[2] + oz), rot(rx, ry, rz)));
        hechas.put(gid, m);
        return m;
    }

    private static float[] rgba(JsonObject p, Estado e, int idx) {
        return rgba(p, e, idx, null);
    }

    /** Color de una pieza; «hex» reemplaza su color (los píxeles de una paleta). */
    private static float[] rgba(JsonObject p, Estado e, int idx, String hex) {
        float alfa = num(p, "alfa", 1);
        float[] c;
        if (bool(p, "arcoiris")) {
            float h = (e.t * num(p, "arcoiris_vel", 0.25f) + idx * num(p, "desfase", 0.06f)) % 1f;
            c = hsv(h < 0 ? h + 1 : h, 0.75f, 1f);
        } else {
            c = color(hex != null ? hex : str(p, "color", "#FFFFFF"));
        }
        JsonObject par = obj(p, "parpadeo");
        if (par != null) {
            float min = num(par, "min", 0.5f);
            float f = min + (1 - min) * (0.5f + 0.5f * (float) Math.sin(2 * Math.PI * num(par, "frec", 1) * e.t
                    + idx * num(par, "desfase", 0)));
            c = new float[]{c[0] * f, c[1] * f, c[2] * f};
        }
        return new float[]{c[0], c[1], c[2], alfa};
    }

    private static boolean cuando(JsonObject p, Estado e) {
        String c = str(p, "cuando", "");
        if (c.equals("aire")) {
            return e.aire > 0.5f;
        }
        if (c.equals("mov")) {
            return e.mov > 0.15f;
        }
        if (c.equals("quieto")) {
            return e.mov <= 0.15f && e.aire <= 0.5f;
        }
        return true;
    }

    private static void agregar(List<Cubo> s, float[][] esquinas, float[] c, boolean brillo) {
        s.add(new Cubo(esquinas, c[0], c[1], c[2], c[3], brillo));
    }

    private static void pieza(JsonObject p, float[] m, Estado e, int k, List<Cubo> s) {
        if (!cuando(p, e)) {
            return;
        }
        boolean brillo = bool(p, "brillo");
        if (p.has("caja")) {
            JsonArray c = p.getAsJsonArray("caja");
            agregar(s, caja(m, c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat(), c.get(3).getAsFloat(),
                    c.get(4).getAsFloat(), c.get(5).getAsFloat()), rgba(p, e, k), brillo);
        } else if (p.has("anillo")) {
            JsonObject a = p.getAsJsonObject("anillo");
            float[] cen = vec(a, "centro", 0, 0, 0), t = vec(a, "tam", 1, 1, 1);
            int n = (int) num(a, "n", 16);
            float r = num(a, "radio", 5);
            for (int i = 0; i < n; i++) {
                float[] mm = mul(m, mul(tras(cen[0], cen[1], cen[2]), mul(rot(0, 360f * i / n, 0), tras(0, 0, r))));
                agregar(s, caja(mm, -t[0] / 2, -t[1] / 2, -t[2] / 2, t[0], t[1], t[2]), rgba(p, e, i), brillo);
            }
        } else if (p.has("pixeles")) {
            JsonObject a = p.getAsJsonObject("pixeles");
            float[] o = vec(a, "origen", 0, 0, 0);
            float tam = num(a, "tam", 1), prof = num(a, "prof", 1);
            String plano = str(a, "plano", "xy");
            JsonObject paleta = obj(a, "paleta");
            JsonArray filas = arr(a, "filas");
            // Si el color no cambia de un píxel a otro, los seguidos del mismo color van en una sola caja
            JsonObject par = obj(p, "parpadeo");
            boolean unir = !bool(p, "arcoiris") && (par == null || num(par, "desfase", 0) == 0);
            for (int fy = 0; fy < filas.size(); fy++) {
                String fila = filas.get(fy).getAsString();
                for (int fx = 0; fx < fila.length(); fx++) {
                    char ch = fila.charAt(fx);
                    String cs = String.valueOf(ch);
                    if (ch == ' ' || ch == '.' || paleta == null || !paleta.has(cs)) {
                        continue;
                    }
                    int n = 1;
                    while (unir && fx + n < fila.length() && fila.charAt(fx + n) == ch) {
                        n++;
                    }
                    float x, y, z, w, h, dd;
                    if (plano.equals("xy")) {
                        x = o[0] + fx * tam; y = o[1] + fy * tam; z = o[2]; w = tam * n; h = tam; dd = prof;
                    } else if (plano.equals("xz")) {
                        x = o[0] + fx * tam; y = o[1]; z = o[2] + fy * tam; w = tam * n; h = prof; dd = tam;
                    } else {
                        x = o[0]; y = o[1] + fy * tam; z = o[2] + fx * tam; w = prof; h = tam; dd = tam * n;
                    }
                    agregar(s, caja(m, x, y, z, w, h, dd), rgba(p, e, fx + fy, paleta.get(cs).getAsString()), brillo);
                    fx += n - 1;
                }
            }
        }
    }

    private static void emisor(JsonObject em, Estado e, int k, List<Cubo> s) {
        String tipo = str(em, "tipo", "");
        float t = e.t;
        JsonArray lc = arr(em, "colores");
        List<float[]> cols = new ArrayList<float[]>();
        for (int i = 0; i < lc.size(); i++) {
            cols.add(color(lc.get(i).getAsString()));
        }
        if (cols.isEmpty()) {
            cols.add(new float[]{1, 1, 1});
        }
        boolean brillo = !em.has("brillo") || em.get("brillo").getAsBoolean();
        float intens = 1 + num(em, "mov", 0) * e.mov + num(em, "golpe", 0) * e.golpe;
        int n = Math.round(num(em, "n", 10) * Math.min(2f, intens));
        float[] id = identidad();
        if (tipo.equals("subida")) {
            float vida = num(em, "vida", 1.2f);
            float[] r = par(em, "radio", 3, 7), y = par(em, "y", 0, 24), tm = par(em, "tam", 2, 0.4f), al = par(em, "alfa", 0.8f, 0);
            float sube = num(em, "subida", 10) * Math.min(1.6f, intens), estirar = num(em, "estirar", 1);
            for (int i = 0; i < n; i++) {
                float fase = hash(k, i, 7);
                long ciclo = (long) Math.floor(t / vida + fase);
                float edad = t / vida + fase - ciclo;
                double ang = hash(k, i, ciclo, 1) * 2 * Math.PI;
                float rad = r[0] + (r[1] - r[0]) * hash(k, i, ciclo, 2);
                float yy = y[1] - (y[1] - y[0]) * hash(k, i, ciclo, 3) - sube * edad;
                float sz = (tm[0] + (tm[1] - tm[0]) * edad) * (0.85f + 0.3f * hash(k, i, ciclo, 4));
                float[] c = cols.get(((int) (hash(k, i, ciclo, 5) * cols.size())) % cols.size());
                float x = (float) Math.cos(ang) * rad, z = (float) Math.sin(ang) * rad, hs = sz * estirar;
                s.add(new Cubo(caja(id, x - sz / 2, yy - hs / 2, z - sz / 2, sz, hs, sz), c[0], c[1], c[2],
                        al[0] + (al[1] - al[0]) * edad, brillo));
            }
        } else if (tipo.equals("orbita")) {
            float r = num(em, "radio", 9), y = num(em, "y", 8), vel = num(em, "vel", 90), sz = num(em, "tam", 1.5f);
            float bob = num(em, "bob", 1);
            for (int i = 0; i < n; i++) {
                double ang = Math.toRadians(vel * t + 360.0 * i / n);
                float yy = y + bob * (float) Math.sin(t * 2.0 + i);
                float[] c = cols.get(i % cols.size());
                float[] m = mul(tras((float) Math.cos(ang) * r, yy, (float) Math.sin(ang) * r), rot(45, vel * t * 2, 45));
                s.add(new Cubo(caja(m, -sz / 2, -sz / 2, -sz / 2, sz, sz, sz), c[0], c[1], c[2], 1, brillo));
            }
        } else if (tipo.equals("caida")) {
            JsonArray a = arr(em, "area");
            float x0 = a.get(0).getAsFloat(), x1 = a.get(1).getAsFloat(), y0 = a.get(2).getAsFloat(), y1 = a.get(3).getAsFloat();
            float z0 = a.get(4).getAsFloat(), z1 = a.get(5).getAsFloat();
            float vida = num(em, "vida", 0.6f), alfa = num(em, "alfa", 0.8f);
            float[] sz = vec(em, "tam", 0.3f, 1.2f, 0.3f);
            for (int i = 0; i < n; i++) {
                float fase = hash(k, i, 3);
                long ciclo = (long) Math.floor(t / vida + fase);
                float edad = t / vida + fase - ciclo;
                float x = x0 + (x1 - x0) * hash(k, i, ciclo, 1), z = z0 + (z1 - z0) * hash(k, i, ciclo, 2);
                float yy = y0 + (y1 - y0) * edad;
                float[] c = cols.get(i % cols.size());
                s.add(new Cubo(caja(id, x - sz[0] / 2, yy, z - sz[2] / 2, sz[0], sz[1], sz[2]), c[0], c[1], c[2],
                        alfa * (1 - edad * 0.5f), brillo));
            }
        } else if (tipo.equals("rayo")) {
            float[] r = par(em, "radio", 4, 8), y = par(em, "y", -2, 24);
            int segs = (int) num(em, "segmentos", 4);
            float sz = num(em, "tam", 0.45f), largo = num(em, "largo", 8), zig = num(em, "zigzag", 3);
            long paso = (long) Math.floor(t * num(em, "frec", 8));
            for (int i = 0; i < n; i++) {
                if (hash(k, i, paso, 9) > num(em, "prob", 0.7f)) {
                    continue;
                }
                double ang = hash(k, i, paso, 1) * 2 * Math.PI;
                float rad = r[0] + (r[1] - r[0]) * hash(k, i, paso, 2);
                float ya = y[0] + (y[1] - y[0]) * hash(k, i, paso, 3);
                float[][] pts = new float[segs + 1][];
                for (int j = 0; j <= segs; j++) {
                    float jit = (hash(k, i, paso, 10 + j) - 0.5f) * zig;
                    double a = ang + jit * 0.08;
                    pts[j] = new float[]{(float) Math.cos(a) * (rad + jit), ya + largo * j / segs, (float) Math.sin(a) * (rad + jit)};
                }
                float[] c = cols.get(i % cols.size());
                for (int j = 0; j < segs; j++) {
                    float dx = pts[j + 1][0] - pts[j][0], dy = pts[j + 1][1] - pts[j][1], dz = pts[j + 1][2] - pts[j][2];
                    float lg = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (lg == 0) {
                        lg = 1;
                    }
                    float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
                    float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, dy / lg))));
                    float[] m = mul(tras(pts[j][0], pts[j][1], pts[j][2]), rot(pitch, yaw, 0));
                    s.add(new Cubo(caja(m, -sz / 2, -sz / 2, 0, sz, sz, lg), c[0], c[1], c[2], 1, brillo));
                }
            }
        }
    }

    /** Ids de todos los cosméticos del catálogo. */
    public static Set<String> ids() {
        Set<String> s = new java.util.LinkedHashSet<String>();
        for (Map.Entry<String, JsonElement> e : defs().entrySet()) {
            if (!e.getKey().startsWith("_")) {
                s.add(e.getKey());
            }
        }
        return s;
    }
}
