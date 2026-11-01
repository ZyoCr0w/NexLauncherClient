package com.nexclient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Formas y colores de la mira. Devuelve rectángulos relativos al centro de la pantalla; cada versión los pinta con lo
 * suyo. Mismo archivo en 1.8.9 y 1.21.1 (NexLauncher dibuja la vista previa con la misma lógica).
 */
public final class Miras {
    public static final String[] ESTILOS = {"Cruz", "Cruz y punto", "Punto", "Círculo", "T", "Círculo y punto", "X",
            "Cuadrado", "Flechas", "Corchetes", "Diamante", "Vanilla", "Propia"};
    public static final int PROPIA = 12;
    /** La mira propia se dibuja en una cuadrícula de 15 × 15 píxeles. */
    public static final int LADO = 15;

    /** Cuándo fue tu último golpe (para el destello). */
    public static volatile long ultimoGolpe;
    private static float hueco;  // apertura dinámica suavizada

    private Miras() {
    }

    /** Rectángulos {x1, y1, x2, y2} alrededor de (0, 0). extra: cuánto se abre la mira dinámica. */
    public static List<int[]> forma(Config c, int extra) {
        int s = c.miraTamano, t = c.miraGrosor, h = c.miraHueco + extra;
        int a = t / 2;
        List<int[]> r = new ArrayList<int[]>();
        switch (c.miraEstilo) {
            case 0: case 1: case 4:
                r.add(new int[]{-h - s, -a, -h, -a + t});
                r.add(new int[]{h + 1, -a, h + 1 + s, -a + t});
                r.add(new int[]{-a, h + 1, -a + t, h + 1 + s});
                if (c.miraEstilo != 4) {
                    r.add(new int[]{-a, -h - s, -a + t, -h});
                }
                if (c.miraEstilo == 1) {
                    punto(r, t);
                }
                break;
            case 2:
                punto(r, Math.max(1, t));
                break;
            case 3: case 5:
                circulo(r, s + h, t);
                if (c.miraEstilo == 5) {
                    punto(r, t);
                }
                break;
            case 6: {  // X
                int d0 = h, d1 = h + s;
                linea(r, d0, d0, d1, d1, t);
                linea(r, -d0, d0, -d1, d1, t);
                linea(r, d0, -d0, d1, -d1, t);
                linea(r, -d0, -d0, -d1, -d1, t);
                break;
            }
            case 7: {  // cuadrado (contorno)
                int m = h + s;
                r.add(new int[]{-m, -m, m + 1, -m + t});
                r.add(new int[]{-m, m + 1 - t, m + 1, m + 1});
                r.add(new int[]{-m, -m, -m + t, m + 1});
                r.add(new int[]{m + 1 - t, -m, m + 1, m + 1});
                break;
            }
            case 8: {  // flechas que apuntan al centro
                int d = h + 1, l = Math.max(2, s / 2 + 1);
                linea(r, -d - l, -l, -d, 0, t);
                linea(r, -d - l, l, -d, 0, t);
                linea(r, d + l, -l, d, 0, t);
                linea(r, d + l, l, d, 0, t);
                break;
            }
            case 9: {  // corchetes [ ]
                int d = h + 2, l = s;
                r.add(new int[]{-d - t, -l, -d, l + 1});
                r.add(new int[]{-d - t, -l, -d + 2, -l + t});
                r.add(new int[]{-d - t, l + 1 - t, -d + 2, l + 1});
                r.add(new int[]{d + 1, -l, d + 1 + t, l + 1});
                r.add(new int[]{d - 1, -l, d + 1 + t, -l + t});
                r.add(new int[]{d - 1, l + 1 - t, d + 1 + t, l + 1});
                break;
            }
            case 10: {  // diamante (contorno)
                int m = h + s;
                linea(r, 0, -m, m, 0, t);
                linea(r, m, 0, 0, m, t);
                linea(r, 0, m, -m, 0, t);
                linea(r, -m, 0, 0, -m, t);
                break;
            }
            case 11: {  // vanilla mejorada: cruz sin hueco, con centro
                int l = Math.max(3, s);
                r.add(new int[]{-l, -a, l + 1, -a + t});
                r.add(new int[]{-a, -l, -a + t, l + 1});
                break;
            }
            default: {  // propia: píxeles dibujados a mano (15 × 15, centro en el 7)
                String p = c.miraPixeles == null ? "" : c.miraPixeles;
                for (int i = 0; i < Math.min(p.length(), LADO * LADO); i++) {
                    if (p.charAt(i) == '1') {
                        int x = i % LADO - LADO / 2, y = i / LADO - LADO / 2;
                        r.add(new int[]{x, y, x + 1, y + 1});
                    }
                }
                break;
            }
        }
        return r;
    }

    /**
     * La mira lista para pintar: {relleno, borde}, cada uno como tramos horizontales {x1, y, x2} que no se pisan
     * (así la opacidad se ve pareja también en círculos y diagonales).
     */
    public static List<List<int[]>> capas(Config c, int extra) {
        Set<Long> relleno = new LinkedHashSet<Long>();
        for (int[] q : forma(c, extra)) {
            for (int y = q[1]; y < q[3]; y++) {
                for (int x = q[0]; x < q[2]; x++) {
                    relleno.add(clave(x, y));
                }
            }
        }
        Set<Long> borde = new LinkedHashSet<Long>();
        if (c.miraBorde) {
            int g = c.miraBordeGrosor;
            for (long k : relleno) {
                int x = (int) (k >> 32), y = (int) k;
                for (int dy = -g; dy <= g; dy++) {
                    for (int dx = -g; dx <= g; dx++) {
                        long n = clave(x + dx, y + dy);
                        if (!relleno.contains(n)) {
                            borde.add(n);
                        }
                    }
                }
            }
        }
        List<List<int[]>> r = new ArrayList<List<int[]>>();
        r.add(tramos(relleno));
        r.add(tramos(borde));
        return r;
    }

    private static long clave(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static List<int[]> tramos(Set<Long> pixeles) {
        List<long[]> l = new ArrayList<long[]>();
        for (long k : pixeles) {
            l.add(new long[]{(int) k, (int) (k >> 32)});  // {y, x}
        }
        java.util.Collections.sort(l, new java.util.Comparator<long[]>() {
            public int compare(long[] a, long[] b) {
                return a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]);
            }
        });
        List<int[]> r = new ArrayList<int[]>();
        int[] actual = null;
        for (long[] p : l) {
            int y = (int) p[0], x = (int) p[1];
            if (actual != null && actual[1] == y && actual[2] == x) {
                actual[2] = x + 1;
            } else {
                actual = new int[]{x, y, x + 1};
                r.add(actual);
            }
        }
        return r;
    }

    private static void punto(List<int[]> r, int d) {
        r.add(new int[]{-d / 2, -d / 2, -d / 2 + d, -d / 2 + d});
    }

    private static void circulo(List<int[]> r, int radio, int t) {
        int a = t / 2;
        Set<Long> hechos = new LinkedHashSet<Long>();
        int pasos = Math.max(24, radio * 8);
        for (int i = 0; i < pasos; i++) {
            double ang = Math.PI * 2 * i / pasos;
            int x = (int) Math.round(Math.cos(ang) * radio), y = (int) Math.round(Math.sin(ang) * radio);
            if (hechos.add(((long) x << 32) ^ (y & 0xFFFFFFFFL))) {
                r.add(new int[]{x - a, y - a, x - a + t, y - a + t});
            }
        }
    }

    /** Línea de grosor t como cuadritos (para X, flechas y diamante). */
    private static void linea(List<int[]> r, int x0, int y0, int x1, int y1, int t) {
        int a = t / 2;
        int pasos = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= pasos; i++) {
            int x = x0 + (pasos == 0 ? 0 : Math.round((x1 - x0) * i / (float) pasos));
            int y = y0 + (pasos == 0 ? 0 : Math.round((y1 - y0) * i / (float) pasos));
            r.add(new int[]{x - a, y - a, x - a + t, y - a + t});
        }
    }

    /**
     * Apertura de la mira dinámica: se abre al moverte rápido o estar en el aire y vuelve suave.
     * velocidad en bloques por segundo.
     */
    public static int apertura(Config c, double velocidad, boolean enAire, float dt) {
        float objetivo = !c.miraDinamica ? 0 : (float) Math.min(6.0, velocidad * 0.6) + (enAire ? 3 : 0);
        hueco += (objetivo - hueco) * Math.min(1f, dt * 12f);
        return Math.round(hueco);
    }

    /** Color ARGB de la mira y de su borde: arcoíris, color al apuntar a un jugador y destello al golpear. */
    public static int[] colores(Config c, boolean apuntando) {
        int alfa = Math.round(c.miraOpacidad * 2.55f) << 24;
        int rgb = c.miraRGB & 0xFFFFFF;
        if (c.miraArcoiris) {
            float tono = (System.currentTimeMillis() % 3000L) / 3000f;
            rgb = tono(tono);
        }
        if (c.miraObjetivo && apuntando) {
            rgb = c.miraObjetivoRGB & 0xFFFFFF;
        }
        if (c.miraDestello) {
            float d = 1f - Math.min(1f, (System.currentTimeMillis() - ultimoGolpe) / 180f);
            if (d > 0) {
                rgb = mezclar(rgb, 0xFFFFFF, d);
            }
        }
        return new int[]{alfa | rgb, alfa | (c.miraBordeRGB & 0xFFFFFF)};
    }

    /** Color del arcoíris (saturación 0.85, brillo 1) sin usar AWT. */
    private static int tono(float h) {
        float s = 0.85f;
        float x = (h * 6f) % 6f;
        int i = (int) x;
        float f = x - i, p = 1 - s, q = 1 - s * f, t = 1 - s * (1 - f);
        float r, g, b;
        switch (i) {
            case 0: r = 1; g = t; b = p; break;
            case 1: r = q; g = 1; b = p; break;
            case 2: r = p; g = 1; b = t; break;
            case 3: r = p; g = q; b = 1; break;
            case 4: r = t; g = p; b = 1; break;
            default: r = 1; g = p; b = q; break;
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private static int mezclar(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ---- compartir: la mira entera como un código corto que se copia y se pega
    private static final String PREFIJO = "NXM:";

    public static String codigo(Config c) {
        String v = c.miraEstilo + "," + c.miraTamano + "," + c.miraGrosor + "," + c.miraHueco + ","
                + Integer.toHexString(c.miraRGB) + "," + c.miraOpacidad + "," + (c.miraBorde ? 1 : 0) + ","
                + Integer.toHexString(c.miraBordeRGB) + "," + c.miraBordeGrosor + "," + (c.miraArcoiris ? 1 : 0) + ","
                + (c.miraDinamica ? 1 : 0) + "," + (c.miraObjetivo ? 1 : 0) + "," + Integer.toHexString(c.miraObjetivoRGB)
                + "," + (c.miraDestello ? 1 : 0) + "," + (c.miraEstilo == PROPIA ? aHex(c.miraPixeles) : "");
        return PREFIJO + Base64.getUrlEncoder().withoutPadding().encodeToString(v.getBytes(StandardCharsets.UTF_8));
    }

    /** Pone en c la mira de un código; false si el código no es válido (y entonces no cambia nada). */
    public static boolean aplicar(Config c, String codigo) {
        try {
            codigo = codigo.trim();
            if (!codigo.startsWith(PREFIJO)) {
                return false;
            }
            String[] v = new String(Base64.getUrlDecoder().decode(codigo.substring(PREFIJO.length())),
                    StandardCharsets.UTF_8).split(",", -1);
            if (v.length < 15) {
                return false;
            }
            int estilo = Integer.parseInt(v[0]), tam = Integer.parseInt(v[1]), grosor = Integer.parseInt(v[2]);
            int hueco = Integer.parseInt(v[3]), rgb = Integer.parseInt(v[4], 16), opac = Integer.parseInt(v[5]);
            int bordeRgb = Integer.parseInt(v[7], 16), bordeG = Integer.parseInt(v[8]);
            int objRgb = Integer.parseInt(v[12], 16);
            c.miraEstilo = estilo;
            c.miraTamano = tam;
            c.miraGrosor = grosor;
            c.miraHueco = hueco;
            c.miraRGB = rgb;
            c.miraOpacidad = opac;
            c.miraBorde = v[6].equals("1");
            c.miraBordeRGB = bordeRgb;
            c.miraBordeGrosor = bordeG;
            c.miraArcoiris = v[9].equals("1");
            c.miraDinamica = v[10].equals("1");
            c.miraObjetivo = v[11].equals("1");
            c.miraObjetivoRGB = objRgb;
            c.miraDestello = v[13].equals("1");
            if (!v[14].isEmpty()) {
                c.miraPixeles = deHex(v[14]);
            }
            c.limitarMira();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String aHex(String bits) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < bits.length(); i += 4) {
            int v = 0;
            for (int j = 0; j < 4; j++) {
                v = v * 2 + (i + j < bits.length() && bits.charAt(i + j) == '1' ? 1 : 0);
            }
            r.append(Integer.toHexString(v));
        }
        return r.toString();
    }

    private static String deHex(String hex) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            int v = Character.digit(hex.charAt(i), 16);
            for (int j = 3; j >= 0; j--) {
                r.append(v >= 0 && ((v >> j) & 1) == 1 ? '1' : '0');
            }
        }
        return r.length() > LADO * LADO ? r.substring(0, LADO * LADO) : r.toString();
    }
}
