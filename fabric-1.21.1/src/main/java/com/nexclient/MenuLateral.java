package com.nexclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Menú principal y de pausa de NexClient: panel oscuro y transparente a la izquierda con el logo, entradas con
 * animación al pasar el ratón y, en el principal, un fondo propio con partículas. Todo se dibuja con rectángulos,
 * texto y el logo de NexClient: ningún paquete de texturas cambia los botones ni el fondo.
 * Este archivo es igual en 1.8.9 y en 1.21.1; cada versión solo pone el «pincel» (cómo dibujar).
 */
public final class MenuLateral {
    /** Lo mínimo para dibujar, en coordenadas de la interfaz (ya escaladas). */
    public interface Pincel {
        void rect(int x1, int y1, int x2, int y2, int argb);

        void degradado(int x1, int y1, int x2, int y2, int arriba, int abajo);

        /** Texto con su esquina superior izquierda en (x, y) y la escala dada. */
        void texto(String s, float x, float y, int argb, float escala, boolean sombra);

        int ancho(String s);

        void logo(int x, int y, int tam, float alfa);

        /** Luz redonda y suave (textura blanca con borde difuminado) estirada a ese rectángulo. */
        void brillo(int x, int y, int w, int h, float alfa);
    }

    public static final class Entrada {
        public final String texto;
        public final Runnable accion;
        public final boolean separador, peligro, activa;
        float hover;

        public Entrada(String texto, Runnable accion, boolean separador, boolean peligro, boolean activa) {
            this.texto = texto;
            this.accion = accion;
            this.separador = separador;
            this.peligro = peligro;
            this.activa = activa;
        }
    }

    public static final int ANCHO = 150;
    private static final int ALTO_FILA = 20, INICIO = 72, HUECO = 9;

    public final List<Entrada> entradas = new ArrayList<Entrada>();
    /** true: menú principal (fondo propio y logo grande); false: pausa (se ve el juego detrás). */
    public final boolean principal;
    /** Fondo propio con partículas (sin mundo detrás) y logo grande a la derecha. */
    public boolean fondoPropio, logoGrande;
    /** La entrada elegida (p. ej. la página abierta en el menú de NexClient): se ve siempre resaltada. */
    public Entrada marcada;
    public String subtitulo = "", pie1 = "", pie2 = "";
    private final long t0 = System.nanoTime();
    private long antes = t0;
    private final float[][] particulas = new float[46][];

    public MenuLateral(boolean principal) {
        this.principal = principal;
        this.fondoPropio = principal;
        this.logoGrande = principal;
        Random r = new Random(7);
        for (int i = 0; i < particulas.length; i++) {
            // x 0..1, y 0..1, velocidad (alto de pantalla por segundo), tamaño, fase del brillo
            particulas[i] = new float[]{r.nextFloat(), r.nextFloat(), 0.012f + r.nextFloat() * 0.03f,
                    r.nextFloat() < 0.25f ? 2 : 1, r.nextFloat() * 6.28f};
        }
    }

    /** Segundos desde que se abrió. */
    public float t() {
        return (System.nanoTime() - t0) / 1e9f;
    }

    private static float suave(float x) {  // easeOutCubic
        x = Math.max(0, Math.min(1, x));
        float u = 1 - x;
        return 1 - u * u * u;
    }

    private static int alfa(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private static int mezclar(int c1, int c2, float k) {
        k = Math.max(0, Math.min(1, k));
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int a = (c1 >>> s) & 0xFF, b = (c2 >>> s) & 0xFF;
            r |= Math.round(a + (b - a) * k) << s;
        }
        return r;
    }

    /** Desplazamiento del panel al abrir (entra deslizando desde la izquierda). */
    private int dx() {
        return Math.round(-ANCHO * (1 - suave(t() / 0.35f)));
    }

    private int yDe(int i) {
        int y = INICIO;
        for (int k = 0; k <= i; k++) {
            if (entradas.get(k).separador) {
                y += HUECO;
            }
            if (k < i) {
                y += ALTO_FILA;
            }
        }
        return y;
    }

    /** La entrada bajo el ratón (activa), o null. */
    public Entrada en(int mx, int my) {
        if (mx < 0 || mx >= ANCHO + dx()) {
            return null;
        }
        for (int i = 0; i < entradas.size(); i++) {
            int y = yDe(i);
            Entrada e = entradas.get(i);
            if (e.activa && my >= y && my < y + ALTO_FILA) {
                return e;
            }
        }
        return null;
    }

    public void dibujar(Pincel p, int w, int h, int mx, int my) {
        long ahora = System.nanoTime();
        float dt = Math.min(0.1f, (ahora - antes) / 1e9f);
        antes = ahora;
        float t = t();
        float aparecer = suave(t / 0.4f);
        if (fondoPropio) {
            fondo(p, w, h, t);
        } else {
            p.rect(0, 0, w, h, alfa(0x8A000000, aparecer));  // el juego se ve detrás, más oscuro
        }
        int dx = dx();
        // panel
        p.degradado(dx, 0, ANCHO + dx, h, fondoPropio ? 0xD80B0B10 : 0xC8050507, fondoPropio ? 0xE6050507 : 0xD8000000);
        p.rect(ANCHO + dx, 0, ANCHO + dx + 1, h, 0x22FFFFFF);
        // cabecera: logo y nombre
        p.logo(12 + dx, 16, 30, aparecer);
        p.texto("§lNexClient", 48 + dx, 19, alfa(0xFFFFFFFF, aparecer), 1.35f, true);
        p.texto(subtitulo, 48 + dx, 35, alfa(0xFF8C8C96, aparecer), 0.75f, false);
        p.rect(12 + dx, 56, ANCHO - 12 + dx, 57, alfa(0x1EFFFFFF, aparecer));
        // entradas
        Entrada bajo = en(mx, my);
        for (int i = 0; i < entradas.size(); i++) {
            Entrada e = entradas.get(i);
            int y = yDe(i);
            if (e.separador && i > 0) {
                p.rect(14 + dx, y - HUECO / 2 - 1, ANCHO - 14 + dx, y - HUECO / 2, 0x14FFFFFF);
            }
            float objetivo = e == bajo || e == marcada ? 1 : 0;
            e.hover += (objetivo - e.hover) * Math.min(1, dt * 14);
            float a = suave((t - 0.10f - i * 0.035f) / 0.28f);  // aparecen una tras otra
            if (a <= 0.02f) {
                continue;
            }
            int ex = dx + Math.round(-10 * (1 - a));
            int acento = e.peligro ? 0xFFFF5A5A : 0xFFFFFFFF;
            if (e.hover > 0.01f) {
                p.degradado(ex, y, ANCHO + dx, y + ALTO_FILA, alfa(e.peligro ? 0x30FF4040 : 0x22FFFFFF, e.hover),
                        alfa(e.peligro ? 0x18FF4040 : 0x10FFFFFF, e.hover));
                int medio = Math.round(8 * e.hover);
                p.rect(ex, y + 10 - medio, ex + 2, y + 10 + medio, alfa(acento, e.hover));
            }
            int base = e.activa ? 0xFFB4B4BE : 0xFF55555C;
            int color = mezclar(base, e.peligro ? 0xFFFF7070 : 0xFFFFFFFF, e.hover);
            p.texto(e.texto, ex + 16 + 4 * e.hover, y + 6, alfa(color, a), 1, false);
        }
        // pie
        p.texto(pie1, 14 + dx, h - 28, alfa(0xFFB4B4BE, aparecer), 0.85f, false);
        p.texto(pie2, 14 + dx, h - 16, alfa(0xFF6E6E78, aparecer), 0.75f, false);
        if (logoGrande) {
            logoGrande(p, w, h, t);
        }
    }

    private void fondo(Pincel p, int w, int h, float t) {
        p.degradado(0, 0, w, h, 0xFF101016, 0xFF040406);
        // luz suave detrás del logo grande, que «respira» despacio
        int cx = ANCHO + (w - ANCHO) / 2, cy = h / 2 - 16;
        int r = Math.min(w - ANCHO, h) * 3 / 4;
        p.brillo(cx - r, cy - r * 3 / 4, 2 * r, r * 3 / 2, (0.10f + 0.03f * (float) Math.sin(t * 0.8f)) * suave(t / 1.0f));
        // partículas que suben despacio y titilan
        for (float[] q : particulas) {
            float y = (q[1] - t * q[2]) % 1f;
            if (y < 0) {
                y += 1;
            }
            int px = Math.round(q[0] * w), py = Math.round(y * h);
            float brillo = 0.55f + 0.45f * (float) Math.sin(t * 1.3f + q[4]);
            int s = (int) q[3];
            p.rect(px, py, px + s, py + s, alfa(0x50FFFFFF, brillo * suave(t / 1.2f)));
        }
    }

    private void logoGrande(Pincel p, int w, int h, float t) {
        float a = suave((t - 0.25f) / 0.6f);
        if (a <= 0.02f) {
            return;
        }
        int cx = ANCHO + (w - ANCHO) / 2;
        float flotar = (float) Math.sin(t * 1.1f) * 3;
        int tam = Math.max(48, Math.min(84, Math.min(w - ANCHO, h) / 4));
        int y = Math.round(h / 2f - tam + flotar - 14 + 10 * (1 - a));
        p.logo(cx - tam / 2, y, tam, a);
        float escala = tam >= 70 ? 3 : 2.4f;
        String nombre = "§lNexClient";
        p.texto(nombre, cx - p.ancho(nombre) * escala / 2f, y + tam + 8, alfa(0xFFFFFFFF, a), escala, true);
        String lema = "PvP · NexLauncher";
        p.texto(lema, cx - p.ancho(lema) / 2f, y + tam + 12 + 9 * escala, alfa(0xFF8C8C96, a), 1, false);
    }
}
