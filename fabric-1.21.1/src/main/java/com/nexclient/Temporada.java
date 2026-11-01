package com.nexclient;

import java.util.Calendar;
import java.util.Random;

/**
 * Temas de temporada de los menús de NexClient. Día de Muertos: del 25 de octubre al 3 de noviembre, con papel picado,
 * pétalos de cempasúchil y calaveritas; se apaga en NexClient ▸ Visual ▸ «Temas de temporada».
 * Todo se dibuja con rectángulos (MenuLateral.Pincel). Este archivo es igual en 1.8.9 y en 1.21.1.
 */
public final class Temporada {
    public static final int CEMPASUCHIL = 0xFFFF8F1F, AMARILLO = 0xFFFFC21A, ROSA = 0xFFE4007C, MORADO = 0xFF8E3CCB,
            TURQUESA = 0xFF19B5C8, VERDE = 0xFF2FB36B;
    /** Los colores del papel picado, en orden. */
    public static final int[] PAPEL = {ROSA, CEMPASUCHIL, MORADO, TURQUESA, AMARILLO, VERDE};

    private static long revisado;
    private static boolean muertos;

    private Temporada() {
    }

    /** ¿Toca el tema de Día de Muertos? (se revisa la fecha cada minuto). */
    public static boolean muertos() {
        Config cfg = Config.get();  // en la pantalla de carga puede no estar leída todavía
        if (cfg != null && !cfg.temaTemporada) {
            return false;
        }
        String forzado = System.getProperty("nexclient.tema", "");
        if (forzado.equals("muertos")) {
            return true;
        }
        if (forzado.equals("normal")) {
            return false;
        }
        long ahora = System.currentTimeMillis();
        if (ahora - revisado > 60000) {
            revisado = ahora;
            Calendar c = Calendar.getInstance();
            int mes = c.get(Calendar.MONTH), dia = c.get(Calendar.DAY_OF_MONTH);
            muertos = (mes == Calendar.OCTOBER && dia >= 25) || (mes == Calendar.NOVEMBER && dia <= 3);
        }
        return muertos;
    }

    /** Color de acento de la entrada i del menú (va cambiando entre los del papel picado). */
    public static int acento(int i) {
        return PAPEL[Math.abs(i) % PAPEL.length];
    }

    public static int alfa(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    // ---------------------------------------------------------------- papel picado
    /** Una banderita (11×10): # papel, . recorte. */
    private static final String[] BANDERA = {
            "###########",
            "#.#######.#",
            "###.###.###",
            "##..#.#..##",
            "###.###.###",
            "#####.#####",
            "###.#.#.###",
            "##.#####.##",
            "###########",
            "#.#.#.#.#.#",
    };

    /** Hilera de papel picado colgando de un hilo entre x1 y x2 (el hilo en y), meciéndose con el tiempo t. */
    public static void papelPicado(MenuLateral.Pincel p, int x1, int x2, int y, float t, float a) {
        if (a <= 0.02f) {
            return;
        }
        p.rect(x1, y, x2, y + 1, alfa(0xFFC9B79C, a * 0.8f));
        int paso = 14;
        int n = Math.max(1, (x2 - x1 - 3) / paso);
        int x0 = x1 + (x2 - x1 - n * paso) / 2 + 2;
        for (int i = 0; i < n; i++) {
            int bx = x0 + i * paso;
            int by = y + 1 + Math.round((float) Math.sin(t * 1.8f + i * 0.9f) * 1.2f + 0.6f);
            int color = alfa(PAPEL[i % PAPEL.length], a * 0.92f);
            for (int fila = 0; fila < BANDERA.length; fila++) {
                String s = BANDERA[fila];
                int inicio = -1;
                for (int c = 0; c <= s.length(); c++) {
                    boolean papel = c < s.length() && s.charAt(c) == '#';
                    if (papel && inicio < 0) {
                        inicio = c;
                    } else if (!papel && inicio >= 0) {
                        p.rect(bx + inicio, by + fila, bx + c, by + fila + 1, color);
                        inicio = -1;
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- calaverita
    private static final String[] CALAVERA = {
            "...WWWWWW...",
            ".WWWWPPWWWW.",
            "WWWWPWWPWWWW",
            "WWWWWPPWWWWW",
            "WKKKWWWWKKKW",
            "KKOKKWWKKOKK",
            "KOYOKWWKOYOK",
            "WKOKWWWWKOKW",
            "WWWWWKKWWWWW",
            ".WWWWKKWWWW.",
            "..WKWKWKWK..",
            "..WWWWWWWW..",
    };

    /** Calaverita de azúcar de 12×12 celdas de tamaño «celda». */
    public static void calavera(MenuLateral.Pincel p, int x, int y, int celda, float a) {
        if (a <= 0.02f) {
            return;
        }
        for (int fila = 0; fila < CALAVERA.length; fila++) {
            String s = CALAVERA[fila];
            int c = 0;
            while (c < s.length()) {
                char k = s.charAt(c);
                int fin = c + 1;
                while (fin < s.length() && s.charAt(fin) == k) {
                    fin++;
                }
                int color = k == 'W' ? 0xFFF4EEE4 : k == 'K' ? 0xFF1C1024 : k == 'O' ? CEMPASUCHIL : k == 'Y' ? AMARILLO
                        : k == 'P' ? ROSA : 0;
                if (color != 0) {
                    p.rect(x + c * celda, y + fila * celda, x + fin * celda, y + (fila + 1) * celda, alfa(color, a));
                }
                c = fin;
            }
        }
    }

    // ---------------------------------------------------------------- flor de cempasúchil
    private static final String[] FLOR = {
            ".YOY.",
            "OYOYO",
            "YOROY",
            "OYOYO",
            ".YOY.",
    };

    public static void flor(MenuLateral.Pincel p, int x, int y, int celda, float a) {
        for (int fila = 0; fila < FLOR.length; fila++) {
            for (int c = 0; c < FLOR[fila].length(); c++) {
                char k = FLOR[fila].charAt(c);
                int color = k == 'Y' ? AMARILLO : k == 'O' ? CEMPASUCHIL : k == 'R' ? 0xFFB8420C : 0;
                if (color != 0) {
                    p.rect(x + c * celda, y + fila * celda, x + (c + 1) * celda, y + (fila + 1) * celda, alfa(color, a));
                }
            }
        }
    }

    // ---------------------------------------------------------------- pétalos que caen
    private static final float[][] PETALOS = new float[60][];

    static {
        Random r = new Random(11);
        for (int i = 0; i < PETALOS.length; i++) {
            // x, y, velocidad de caída, amplitud del vaivén, fase, tamaño, color
            PETALOS[i] = new float[]{r.nextFloat(), r.nextFloat(), 0.025f + r.nextFloat() * 0.035f, 4 + r.nextFloat() * 10,
                    r.nextFloat() * 6.28f, r.nextFloat() < 0.3f ? 3 : 2, r.nextInt(10)};
        }
    }

    /** Pétalos de cempasúchil (y alguno rosa) cayendo y meciéndose en el rectángulo dado. */
    public static void petalos(MenuLateral.Pincel p, int x1, int y1, int w, int h, float t, float a) {
        for (float[] q : PETALOS) {
            float y = (q[1] + t * q[2]) % 1f;
            int px = x1 + Math.round(q[0] * w + (float) Math.sin(t * 0.9f + q[4]) * q[3]);
            int py = y1 + Math.round(y * h);
            int s = (int) q[5];
            boolean girado = Math.sin(t * 2.5f + q[4] * 3) > 0;  // «gira»: a ratos más ancho que alto
            int c = q[6] < 6 ? CEMPASUCHIL : q[6] < 9 ? AMARILLO : ROSA;
            float borde = Math.min(1, Math.min(y * 8, (1 - y) * 8));
            p.rect(px, py, px + (girado ? s + 1 : s), py + (girado ? s : s + 1), alfa(c, a * 0.75f * borde));
        }
    }
}
