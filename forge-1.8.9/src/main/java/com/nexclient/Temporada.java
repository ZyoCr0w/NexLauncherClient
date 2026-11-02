package com.nexclient;

import java.util.Calendar;
import java.util.Random;

/**
 * Festividades de NexClient: decoran los menús y la pantalla de carga, y activan sus cosméticos especiales.
 * <ul>
 * <li>Fiestas Patrias: del 1 al 16 de septiembre y del 4 al 20 de noviembre (papel picado verde, blanco y rojo,
 * confeti tricolor, banderitas y sombrero de charro).</li>
 * <li>Día de Muertos: del 25 de octubre al 3 de noviembre (papel picado de colores, pétalos de cempasúchil y
 * calaveritas).</li>
 * <li>Navidad: del 1 de diciembre al 6 de enero (series de luces, nieve, regalos y arbolito).</li>
 * </ul>
 * La decoración se apaga en NexClient ▸ Visual ▸ «Temas de temporada»; los cosméticos de temporada dependen solo de
 * la fecha. Todo se dibuja con rectángulos (MenuLateral.Pincel). Este archivo es igual en 1.8.9 y en 1.21.1.
 */
public final class Temporada {
    public static final int NINGUNA = 0, MUERTOS = 1, PATRIAS = 2, NAVIDAD = 3;
    public static final int CEMPASUCHIL = 0xFFFF8F1F, AMARILLO = 0xFFFFC21A, ROSA = 0xFFE4007C, MORADO = 0xFF8E3CCB,
            TURQUESA = 0xFF19B5C8, VERDE = 0xFF2FB36B;
    public static final int VERDE_MX = 0xFF1E9E4A, BLANCO_MX = 0xFFF4F4F0, ROJO_MX = 0xFFD62828;
    public static final int ROJO_NAV = 0xFFD7263D, VERDE_NAV = 0xFF1E7B3C, ORO = 0xFFFFD54A;
    /** Los colores del papel picado de Día de Muertos, en orden. */
    public static final int[] PAPEL = {ROSA, CEMPASUCHIL, MORADO, TURQUESA, AMARILLO, VERDE};
    private static final int[] PAPEL_PATRIAS = {VERDE_MX, BLANCO_MX, ROJO_MX};
    private static final int[] LUCES = {0xFFFF3B3B, 0xFFFFD54A, 0xFF3BD16F, 0xFF4FA3FF, 0xFFFF7AD9};

    private static long revisado;
    private static int porFecha;

    private Temporada() {
    }

    /** Festividad según la fecha (sin importar el ajuste de decoración). -Dnexclient.tema=muertos|patrias|navidad|normal. */
    public static int porFecha() {
        String forzado = System.getProperty("nexclient.tema", "");
        if (forzado.equals("muertos")) {
            return MUERTOS;
        }
        if (forzado.equals("patrias")) {
            return PATRIAS;
        }
        if (forzado.equals("navidad")) {
            return NAVIDAD;
        }
        if (forzado.equals("normal")) {
            return NINGUNA;
        }
        long ahora = System.currentTimeMillis();
        if (ahora - revisado > 60000) {
            revisado = ahora;
            Calendar c = Calendar.getInstance();
            porFecha = deFecha(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        }
        return porFecha;
    }

    /** Mes 1–12 y día → festividad. */
    public static int deFecha(int mes, int dia) {
        if ((mes == 10 && dia >= 25) || (mes == 11 && dia <= 3)) {
            return MUERTOS;
        }
        if ((mes == 9 && dia <= 16) || (mes == 11 && dia >= 4 && dia <= 20)) {
            return PATRIAS;
        }
        if (mes == 12 || (mes == 1 && dia <= 6)) {
            return NAVIDAD;
        }
        return NINGUNA;
    }

    /** Festividad que decora los menús (NINGUNA si se apagaron los temas de temporada). */
    public static int actual() {
        Config cfg = Config.get();  // en la pantalla de carga puede no estar leída todavía
        if (cfg != null && !cfg.temaTemporada) {
            return NINGUNA;
        }
        return porFecha();
    }

    /** ¿Los menús van decorados? */
    public static boolean activa() {
        return actual() != NINGUNA;
    }

    public static boolean muertos() {
        return actual() == MUERTOS;
    }

    /** ¿Se ven los cosméticos de esa temporada («muertos», «patrias», «navidad»)? Solo depende de la fecha. */
    public static boolean vigente(String temporada) {
        int f = porFecha();
        return ("muertos".equals(temporada) && f == MUERTOS) || ("patrias".equals(temporada) && f == PATRIAS)
                || ("navidad".equals(temporada) && f == NAVIDAD);
    }

    public static String nombre() {
        int f = actual();
        return f == MUERTOS ? "Día de Muertos" : f == PATRIAS ? "Fiestas Patrias" : f == NAVIDAD ? "Navidad" : "";
    }

    // ---------------------------------------------------------------- colores de cada festividad
    public static int[] papel() {
        return actual() == PATRIAS ? PAPEL_PATRIAS : actual() == NAVIDAD ? LUCES : PAPEL;
    }

    /** Color de acento de la entrada i del menú (va cambiando entre los de la festividad). */
    public static int acento(int i) {
        int[] p = papel();
        return p[Math.abs(i) % p.length];
    }

    /** Acento principal (líneas, barras): cempasúchil, verde o rojo navideño. */
    public static int principal() {
        int f = actual();
        return f == PATRIAS ? VERDE_MX : f == NAVIDAD ? ROJO_NAV : CEMPASUCHIL;
    }

    /** Acento claro (perillas al pasar el ratón). */
    public static int secundario() {
        int f = actual();
        return f == PATRIAS ? BLANCO_MX : f == NAVIDAD ? ORO : AMARILLO;
    }

    /** Color de los textos de la festividad (el nombre bajo el logo). */
    public static int textoTema() {
        int f = actual();
        return f == PATRIAS ? 0xFF8FE3AA : f == NAVIDAD ? 0xFFFF9C9C : 0xFFFFB45A;
    }

    /** Degradado de fondo de las pantallas sin mundo (arriba y abajo). */
    public static int fondoArriba() {
        int f = actual();
        return f == PATRIAS ? 0xFF0C1E14 : f == NAVIDAD ? 0xFF0C1830 : 0xFF1C0A28;
    }

    public static int fondoAbajo() {
        int f = actual();
        return f == PATRIAS ? 0xFF040906 : f == NAVIDAD ? 0xFF03060E : 0xFF06030A;
    }

    /** Tinte (RGB) de paneles y botones. */
    public static int tinte() {
        int f = actual();
        return f == PATRIAS ? 0x0C1C12 : f == NAVIDAD ? 0x0A1426 : 0x180A22;
    }

    public static int tinteClaro() {
        int f = actual();
        return f == PATRIAS ? 0x1C3C28 : f == NAVIDAD ? 0x1C2C4C : 0x301A3A;
    }

    /** Fondo de la pantalla de carga (RGB). */
    public static int fondoCarga() {
        int f = actual();
        return f == PATRIAS ? 0x0A1A10 : f == NAVIDAD ? 0x0A1222 : 0x170921;
    }

    public static int alfa(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private static void figura(MenuLateral.Pincel p, String[] filas, int x, int y, int celda, float a, int[] colores,
                               String claves) {
        for (int fila = 0; fila < filas.length; fila++) {
            String s = filas[fila];
            int c = 0;
            while (c < s.length()) {
                char k = s.charAt(c);
                int fin = c + 1;
                while (fin < s.length() && s.charAt(fin) == k) {
                    fin++;
                }
                int i = claves.indexOf(k);
                if (i >= 0) {
                    p.rect(x + c * celda, y + fila * celda, x + fin * celda, y + (fila + 1) * celda, alfa(colores[i], a));
                }
                c = fin;
            }
        }
    }

    // ---------------------------------------------------------------- guirnalda: papel picado o serie de luces
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

    /**
     * Guirnalda colgando de un hilo entre x1 y x2 (el hilo en y), meciéndose con el tiempo t: papel picado (de
     * colores en Día de Muertos, tricolor en Fiestas Patrias) o una serie de foquitos que titilan en Navidad.
     */
    public static void papelPicado(MenuLateral.Pincel p, int x1, int x2, int y, float t, float a) {
        if (a <= 0.02f) {
            return;
        }
        if (actual() == NAVIDAD) {
            luces(p, x1, x2, y, t, a);
            return;
        }
        int[] colores = papel();
        p.rect(x1, y, x2, y + 1, alfa(0xFFC9B79C, a * 0.8f));
        int paso = 14;
        int n = Math.max(1, (x2 - x1 - 3) / paso);
        int x0 = x1 + (x2 - x1 - n * paso) / 2 + 2;
        for (int i = 0; i < n; i++) {
            int bx = x0 + i * paso;
            int by = y + 1 + Math.round((float) Math.sin(t * 1.8f + i * 0.9f) * 1.2f + 0.6f);
            int color = alfa(colores[i % colores.length], a * 0.92f);
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

    /** Serie de foquitos navideños: un cable que cuelga en curvas y focos de colores que se prenden a destiempo. */
    private static void luces(MenuLateral.Pincel p, int x1, int x2, int y, float t, float a) {
        int paso = 12;
        int n = Math.max(1, (x2 - x1) / paso);
        int cable = alfa(0xFF2E4A2E, a * 0.9f);
        for (int x = x1; x < x2; x++) {
            int cy = y + Math.round((float) Math.abs(Math.sin((x - x1) * Math.PI / paso)) * 3);
            p.rect(x, cy, x + 1, cy + 1, cable);
        }
        for (int i = 0; i < n; i++) {
            int bx = x1 + i * paso + paso / 2 - 2;
            int by = y + 3;
            int c = LUCES[i % LUCES.length];
            float brillo = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(t * 2.6f + i * 1.9f));
            p.rect(bx + 1, by, bx + 3, by + 1, alfa(0xFF5A5A5A, a));        // casquillo
            p.rect(bx, by + 1, bx + 4, by + 6, alfa(c, a * brillo));
            p.rect(bx + 1, by + 6, bx + 3, by + 7, alfa(c, a * brillo));
            p.rect(bx + 1, by + 2, bx + 2, by + 3, alfa(0xFFFFFFFF, a * brillo * 0.8f));
            p.rect(bx - 2, by - 1, bx + 6, by + 9, alfa(c, a * brillo * 0.13f));  // halo
        }
    }

    // ---------------------------------------------------------------- figura junto al logo
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
    private static final String[] SOMBRERO = {
            "............",
            ".....NN.....",
            "....NNNN....",
            "....NNNN....",
            "...NNNNNN...",
            "...VVBBRR...",
            "...NNNNNN...",
            "OONNNNNNNNOO",
            ".OONNGGNNOO.",
            "..OOOOOOOO..",
            "............",
            "............",
    };
    private static final String[] ARBOL = {
            ".....Y......",
            ".....V......",
            "....VVV.....",
            "...VRVVV....",
            "....VVV.....",
            "...VVVYV....",
            "..VBVVVVV...",
            "...VVVVR....",
            "..VVYVVVVV..",
            ".VVVVVVBVVV.",
            ".....CC.....",
            ".....CC.....",
    };

    /** Figura de la festividad (12×12 celdas): calaverita, sombrero de charro o arbolito de Navidad. */
    public static void calavera(MenuLateral.Pincel p, int x, int y, int celda, float a) {
        if (a <= 0.02f) {
            return;
        }
        int f = actual();
        if (f == PATRIAS) {
            figura(p, SOMBRERO, x, y, celda, a, new int[]{0xFF231A16, VERDE_MX, BLANCO_MX, ROJO_MX, 0xFFFFD54A, 0xFFE0B23A},
                    "NVBROG");
        } else if (f == NAVIDAD) {
            float luz = 0.6f + 0.4f * (float) Math.sin(System.nanoTime() / 2.5e8);
            figura(p, ARBOL, x, y, celda, a, new int[]{0xFF2E8B3E, alfa(ORO, luz), 0xFFE53935, 0xFF4FA3FF, 0xFF6D4C2F},
                    "VYRBC");
        } else {
            figura(p, CALAVERA, x, y, celda, a, new int[]{0xFFF4EEE4, 0xFF1C1024, CEMPASUCHIL, AMARILLO, ROSA}, "WKOYP");
        }
    }

    // ---------------------------------------------------------------- adornos al pie
    private static final String[] FLOR = {".YOY.", "OYOYO", "YOROY", "OYOYO", ".YOY."};
    private static final String[] BANDERITA = {"P.....", "PVVBRR", "PVVBRR", "PVVBRR", "P.....", "P....."};
    private static final String[] REGALO = {"..O.O.", "...O..", "RRROROR", "RRROROR", "OOOOOOO", "RRROROR", "RRROROR"};

    public static void flor(MenuLateral.Pincel p, int x, int y, int celda, float a) {
        adorno(p, x, y, celda, a, 0);
    }

    /** Adorno de la fila de abajo (k = su posición): cempasúchil, banderita de México, o regalo y arbolito. */
    public static void adorno(MenuLateral.Pincel p, int x, int y, int celda, float a, int k) {
        int f = actual();
        if (f == PATRIAS) {
            figura(p, BANDERITA, x, y - celda, celda, a, new int[]{0xFF8A7560, VERDE_MX, BLANCO_MX, ROJO_MX}, "PVBR");
        } else if (f == NAVIDAD) {
            int[] cajas = {ROJO_NAV, 0xFF2E7DD7, VERDE_NAV, 0xFF8E3CCB};
            figura(p, REGALO, x, y - celda, celda, a, new int[]{cajas[Math.abs(k) % cajas.length], ORO}, "RO");
        } else {
            figura(p, FLOR, x, y, celda, a, new int[]{AMARILLO, CEMPASUCHIL, 0xFFB8420C}, "YOR");
        }
    }

    // ---------------------------------------------------------------- lo que cae: pétalos, confeti o nieve
    private static final float[][] PETALOS = new float[60][];

    static {
        Random r = new Random(11);
        for (int i = 0; i < PETALOS.length; i++) {
            // x, y, velocidad de caída, amplitud del vaivén, fase, tamaño, color
            PETALOS[i] = new float[]{r.nextFloat(), r.nextFloat(), 0.025f + r.nextFloat() * 0.035f, 4 + r.nextFloat() * 10,
                    r.nextFloat() * 6.28f, r.nextFloat() < 0.3f ? 3 : 2, r.nextInt(10)};
        }
    }

    /** Pétalos de cempasúchil, confeti verde-blanco-rojo o copos de nieve, cayendo y meciéndose en el rectángulo. */
    public static void petalos(MenuLateral.Pincel p, int x1, int y1, int w, int h, float t, float a) {
        int f = actual();
        for (float[] q : PETALOS) {
            float vel = f == NAVIDAD ? q[2] * 0.6f : q[2];
            float y = (q[1] + t * vel) % 1f;
            int px = x1 + Math.round(q[0] * w + (float) Math.sin(t * 0.9f + q[4]) * q[3]);
            int py = y1 + Math.round(y * h);
            int s = (int) q[5];
            boolean girado = Math.sin(t * 2.5f + q[4] * 3) > 0;  // «gira»: a ratos más ancho que alto
            float borde = Math.min(1, Math.min(y * 8, (1 - y) * 8));
            int c;
            if (f == NAVIDAD) {
                c = q[6] < 8 ? 0xFFFFFFFF : 0xFFCFE8FF;
                p.rect(px, py, px + s, py + s, alfa(c, a * 0.8f * borde));
                continue;
            }
            if (f == PATRIAS) {
                c = q[6] < 4 ? VERDE_MX : q[6] < 7 ? BLANCO_MX : ROJO_MX;
            } else {
                c = q[6] < 6 ? CEMPASUCHIL : q[6] < 9 ? AMARILLO : ROSA;
            }
            p.rect(px, py, px + (girado ? s + 1 : s), py + (girado ? s : s + 1), alfa(c, a * 0.75f * borde));
        }
    }
}
