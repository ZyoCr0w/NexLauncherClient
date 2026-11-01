package com.nexclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Capas animadas como cuadros listos para subir a la tarjeta de video: el GIF de un jugador pasado al molde de capa
 * de Minecraft, y las capas animadas de Nex hechas por código. Mismo archivo en 1.8.9 y 1.21.1 (no usa Minecraft).
 */
public final class CapasAnimadas {
    /** Cuadros ARGB de ancho × alto (el molde de capa es 64 × 32, aquí multiplicado por la escala). */
    public static final class Animacion {
        public final int ancho, alto;
        public final List<int[]> cuadros = new ArrayList<int[]>();
        public final List<Integer> tiempos = new ArrayList<Integer>();
        public int total;

        Animacion(int ancho, int alto) {
            this.ancho = ancho;
            this.alto = alto;
        }

        void agregar(int[] c, int ms) {
            cuadros.add(c);
            tiempos.add(Math.max(20, ms));
            total += Math.max(20, ms);
        }

        /** Qué cuadro toca ahora (todas las capas iguales se mueven a la vez). */
        public int indice(long ahora) {
            if (cuadros.size() <= 1 || total <= 0) {
                return 0;
            }
            long t = ahora % total;
            for (int i = 0; i < tiempos.size(); i++) {
                t -= tiempos.get(i);
                if (t < 0) {
                    return i;
                }
            }
            return cuadros.size() - 1;
        }
    }

    private CapasAnimadas() {
    }

    /**
     * El GIF de un jugador como capa. Si mide el doble de ancho que de alto, ya es un molde de capa (64 × 32 o más
     * grande). Si no, es un dibujo: va en la cara de afuera de la capa (y en la élitra), recortado al centro.
     */
    public static Animacion deGif(Gif g) {
        boolean molde = Math.abs(g.ancho - g.alto * 2) <= Math.max(1, g.alto / 16);
        int k = molde ? clamp(Math.round(g.alto / 32f), 1, 4) : clamp(Math.round(Math.min(g.alto / 16f, g.ancho / 10f)), 1, 4);
        Animacion a = new Animacion(64 * k, 32 * k);
        for (int i = 0; i < g.cuadros.size(); i++) {
            int[] src = g.cuadros.get(i);
            int[] dst = new int[a.ancho * a.alto];
            if (molde) {
                escalar(src, g.ancho, g.alto, 0, 0, g.ancho, g.alto, dst, a.ancho, 0, 0, a.ancho, a.alto, false);
            } else {
                dibujo(src, g.ancho, g.alto, dst, a.ancho, k);
            }
            a.agregar(dst, g.tiempos.get(i));
        }
        return a;
    }

    /** Coloca un dibujo en la cara de afuera (1,1)-(11,17), la de adentro, los bordes y la élitra. */
    private static void dibujo(int[] src, int sw, int sh, int[] dst, int dw, int k) {
        // recorte al centro con la proporción 10:16 de la capa
        int cw = sw, ch = sh;
        if (sw * 16 > sh * 10) {
            cw = sh * 10 / 16;
        } else {
            ch = sw * 16 / 10;
        }
        int cx = (sw - cw) / 2, cy = (sh - ch) / 2;
        escalar(src, sw, sh, cx, cy, cw, ch, dst, dw, k, k, 10 * k, 16 * k, false);            // afuera
        escalar(src, sw, sh, cx, cy, cw, ch, dst, dw, 12 * k, k, 10 * k, 16 * k, true);       // adentro (espejo)
        oscurecer(dst, dw, 12 * k, k, 10 * k, 16 * k, 0.6f);
        // bordes: repiten el píxel del borde del dibujo
        for (int y = k; y < 17 * k; y++) {
            for (int x = 0; x < k; x++) {
                dst[y * dw + x] = dst[y * dw + k];                      // lado izquierdo
                dst[y * dw + 11 * k + x] = dst[y * dw + 11 * k - 1];    // lado derecho
            }
        }
        for (int x = 0; x < 10 * k; x++) {
            for (int y = 0; y < k; y++) {
                dst[y * dw + k + x] = dst[k * dw + k + x];              // arriba
                dst[y * dw + 11 * k + x] = dst[(17 * k - 1) * dw + k + x];  // abajo
            }
        }
        // élitra (22,0)-(46,22): el mismo dibujo
        escalar(src, sw, sh, cx, cy, cw, ch, dst, dw, 22 * k, 0, 24 * k, 22 * k, false);
    }

    /** Vecino más cercano de un recorte de src a un rectángulo de dst (espejo: volteado en horizontal). */
    private static void escalar(int[] src, int sw, int sh, int sx, int sy, int cw, int ch, int[] dst, int dw,
                                int dx, int dy, int w, int h, boolean espejo) {
        for (int y = 0; y < h; y++) {
            int yy = sy + Math.min(ch - 1, y * ch / h);
            for (int x = 0; x < w; x++) {
                int xx = sx + Math.min(cw - 1, (espejo ? w - 1 - x : x) * cw / w);
                int v = src[yy * sw + xx];
                dst[(dy + y) * dw + dx + x] = (v >>> 24) < 16 ? 0 : (0xFF000000 | v);  // la capa no admite semitransparencia
            }
        }
    }

    private static void oscurecer(int[] dst, int dw, int x0, int y0, int w, int h, float f) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                int v = dst[y * dw + x];
                if (v != 0) {
                    dst[y * dw + x] = 0xFF000000 | (Math.round(((v >> 16) & 255) * f) << 16)
                            | (Math.round(((v >> 8) & 255) * f) << 8) | Math.round((v & 255) * f);
                }
            }
        }
    }

    private static int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }

    // ---- capas animadas de Nex (hechas por código)
    /** Una «N» de 6 × 8 píxeles para el logo. */
    private static final String[] N = {"110011", "111011", "111011", "110111", "110111", "110011", "110011", "110011"};

    private static int[] base(int k, int fondo, int borde, int logo) {
        int w = 64 * k, h = 32 * k;
        int[] d = new int[w * h];
        for (int y = 0; y < 17; y++) {
            for (int x = 0; x < 23; x++) {
                boolean cara = y >= 1 && ((x >= 1 && x < 11) || (x >= 12 && x < 22));
                boolean lado = (x == 0 || x == 11) && y >= 1 || y == 0 && x >= 1 && x < 21;
                if (!cara && !lado) {
                    continue;
                }
                boolean marco = cara && (x == 1 || x == 10 || y == 1 || y == 16 || x == 12 || x == 21);
                int c = lado || marco ? borde : fondo;
                for (int yy = 0; yy < k; yy++) {
                    for (int xx = 0; xx < k; xx++) {
                        d[(y * k + yy) * w + x * k + xx] = c;
                    }
                }
            }
        }
        for (int y = 0; y < 22; y++) {  // élitra (22,0)-(46,22): del mismo color, si no se vería invisible
            for (int x = 22; x < 46; x++) {
                int c = y == 0 || y == 21 || x == 22 || x == 45 ? borde : fondo;
                for (int yy = 0; yy < k; yy++) {
                    for (int xx = 0; xx < k; xx++) {
                        d[(y * k + yy) * w + x * k + xx] = c;
                    }
                }
            }
        }
        for (int y = 0; y < 8; y++) {  // logo en la cara de afuera, al centro
            for (int x = 0; x < 6; x++) {
                if (N[y].charAt(x) == '1') {
                    for (int yy = 0; yy < k; yy++) {
                        for (int xx = 0; xx < k; xx++) {
                            d[((5 + y) * k + yy) * w + (3 + x) * k + xx] = logo;
                        }
                    }
                }
            }
        }
        return d;
    }

    /** Capa negra con la N verde y un brillo que la cruza en diagonal cada 2.4 s. */
    public static Animacion brillo() {
        int k = 4, w = 64 * k;
        Animacion a = new Animacion(w, 32 * k);
        int[] fondo = base(k, 0xFF141416, 0xFF2BD96B, 0xFF2BD96B);
        int pasos = 28;
        for (int f = 0; f < pasos; f++) {
            int[] d = fondo.clone();
            float banda = -8 * k + (f / (float) (pasos - 1)) * (26 * k + 16 * k);  // recorre la cara de afuera
            for (int y = k; y < 17 * k; y++) {
                for (int x = k; x < 11 * k; x++) {
                    float dist = Math.abs((x + (y - k) * 0.6f) - banda);
                    if (dist < 3 * k) {
                        float t = 0.75f * (1 - dist / (3 * k));
                        d[y * w + x] = mezclar(d[y * w + x], 0xFFFFFFFF, t);
                    }
                }
            }
            a.agregar(d, 45);
        }
        a.agregar(fondo, 1100);  // pausa sin brillo
        return a;
    }

    /** Capa que va cambiando de color (degradado que avanza), con la N blanca. */
    public static Animacion color() {
        int k = 4, w = 64 * k;
        Animacion a = new Animacion(w, 32 * k);
        int[] molde = base(k, 0xFF808080, 0xFF404040, 0xFFFFFFFF);
        int pasos = 36;
        for (int f = 0; f < pasos; f++) {
            int[] d = molde.clone();
            for (int y = 0; y < 17 * k; y++) {
                for (int x = 0; x < 23 * k; x++) {
                    int v = d[y * w + x];
                    if (v == 0 || v == 0xFFFFFFFF) {
                        continue;
                    }
                    float tono = (f / (float) pasos + y / (float) (17 * k) * 0.35f) % 1f;
                    int c = hsv(tono, 0.75f, v == 0xFF404040 ? 0.55f : 0.95f);
                    d[y * w + x] = c;
                }
            }
            a.agregar(d, 70);
        }
        return a;
    }

    private static int hsv(float h, float s, float v) {
        float x = (h * 6f) % 6f;
        int i = (int) x;
        float f = x - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private static int mezclar(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
