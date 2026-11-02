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
        /** Submenú (Opciones → Video, Sonido…): se despliega en el mismo panel al tocar la entrada. */
        public final List<Entrada> hijos = new ArrayList<Entrada>();
        public boolean abierto;
        float hover, apertura;

        public Entrada(String texto, Runnable accion, boolean separador, boolean peligro, boolean activa) {
            this.texto = texto;
            this.accion = accion;
            this.separador = separador;
            this.peligro = peligro;
            this.activa = activa;
        }
    }

    public static final int ANCHO = 150;
    private static final int ALTO_FILA = 18, ALTO_HIJO = 15, INICIO = 66, HUECO = 7;

    /** Una fila visible del panel en este cuadro: la entrada, dónde va, su alto y qué tanto se ve (submenús). */
    private static final class Fila {
        final Entrada e;
        final int y, alto;
        final float vis;
        final boolean hijo;

        Fila(Entrada e, int y, int alto, float vis, boolean hijo) {
            this.e = e;
            this.y = y;
            this.alto = alto;
            this.vis = vis;
            this.hijo = hijo;
        }
    }

    private final List<Fila> filas = new ArrayList<Fila>();

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

    /** Las filas de este cuadro: las entradas y, debajo de las abiertas, sus submenús (que se abren de a poco). */
    private void calcular(float dt) {
        filas.clear();
        int y = INICIO;
        for (int i = 0; i < entradas.size(); i++) {
            Entrada e = entradas.get(i);
            if (e.separador && i > 0) {
                y += HUECO;
            }
            filas.add(new Fila(e, y, ALTO_FILA, 1, false));
            y += ALTO_FILA;
            e.apertura += ((e.abierto ? 1 : 0) - e.apertura) * Math.min(1, dt * 12);
            if (!e.hijos.isEmpty() && e.apertura > 0.01f) {
                for (Entrada c : e.hijos) {
                    int alto = Math.round(ALTO_HIJO * e.apertura);
                    filas.add(new Fila(c, y, alto, e.apertura, true));
                    y += alto;
                }
            }
        }
    }

    /** Toca una entrada con submenú: se abre (y se cierran las demás) o se cierra. */
    public void alternar(Entrada e) {
        boolean abrir = !e.abierto;
        for (Entrada x : entradas) {
            x.abierto = false;
        }
        e.abierto = abrir;
    }

    /** La entrada bajo el ratón (activa), o null. */
    public Entrada en(int mx, int my) {
        if (mx < 0 || mx >= ANCHO + dx()) {
            return null;
        }
        if (filas.isEmpty()) {
            calcular(0);
        }
        for (Fila f : filas) {
            if (f.e.activa && f.vis > 0.9f && my >= f.y && my < f.y + f.alto) {
                return f.e;
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
        boolean dm = Temporada.activa();
        if (fondoPropio) {
            fondo(p, w, h, t, dm);
        } else {
            p.rect(0, 0, w, h, alfa(dm ? 0x8A000000 | Temporada.tinte() : 0x8A000000, aparecer));  // el juego se ve detrás, más oscuro
        }
        int dx = dx();
        // panel
        if (dm) {
            p.degradado(dx, 0, ANCHO + dx, h, (fondoPropio ? 0xE0000000 : 0xD0000000) | Temporada.tinte(),
                    (fondoPropio ? 0xEC000000 : 0xE0000000) | (Temporada.fondoAbajo() & 0xFFFFFF));
            p.rect(ANCHO + dx, 0, ANCHO + dx + 1, h, alfa(Temporada.principal(), 0.45f));
            Temporada.papelPicado(p, dx, ANCHO + dx, 1, t, aparecer);
        } else {
            p.degradado(dx, 0, ANCHO + dx, h, fondoPropio ? 0xD80B0B10 : 0xC8050507, fondoPropio ? 0xE6050507 : 0xD8000000);
            p.rect(ANCHO + dx, 0, ANCHO + dx + 1, h, 0x22FFFFFF);
        }
        // cabecera: logo y nombre
        int yc = dm ? 4 : 0;  // con papel picado arriba, la cabecera baja un poco
        p.logo(12 + dx, 16 + yc, 30, aparecer);
        p.texto("§lNexClient", 48 + dx, 19 + yc, alfa(0xFFFFFFFF, aparecer), 1.35f, true);
        p.texto(dm ? Tr.t(Temporada.nombre()) : Tr.t(subtitulo), 48 + dx, 35 + yc,
                alfa(dm ? Temporada.textoTema() : 0xFF8C8C96, aparecer), 0.75f, false);
        p.rect(12 + dx, 56 + yc, ANCHO - 12 + dx, 57 + yc, alfa(dm ? 0x66000000 | (Temporada.principal() & 0xFFFFFF) : 0x1EFFFFFF, aparecer));
        // entradas (y submenús abiertos)
        calcular(dt);
        Entrada bajo = en(mx, my);
        int i = 0;
        for (Fila f : filas) {
            Entrada e = f.e;
            int y = f.y;
            if (!f.hijo && e.separador && entradas.indexOf(e) > 0) {
                p.rect(14 + dx, y - HUECO / 2 - 1, ANCHO - 14 + dx, y - HUECO / 2, dm ? 0x30000000 | (Temporada.principal() & 0xFFFFFF) : 0x14FFFFFF);
            }
            float objetivo = e == bajo || e == marcada ? 1 : 0;
            e.hover += (objetivo - e.hover) * Math.min(1, dt * 14);
            float a = suave((t - 0.10f - i++ * 0.03f) / 0.28f) * (f.hijo ? suave((f.vis - 0.35f) / 0.65f) : 1);
            if (a <= 0.02f) {
                continue;
            }
            int ex = dx + Math.round(-10 * (1 - a));
            int acento = e.peligro ? 0xFFFF5A5A : dm ? Temporada.acento(i) : 0xFFFFFFFF;
            if (e.hover > 0.01f) {
                int c1 = e.peligro ? 0x30FF4040 : dm ? (0x38000000 | (acento & 0xFFFFFF)) : 0x22FFFFFF;
                int c2 = e.peligro ? 0x18FF4040 : dm ? (0x0C000000 | (acento & 0xFFFFFF)) : 0x10FFFFFF;
                p.degradado(ex, y, ANCHO + dx, y + f.alto, alfa(c1, e.hover * a), alfa(c2, e.hover * a));
                int medio = Math.round((f.alto / 2f - 2) * e.hover);
                p.rect(ex + (f.hijo ? 14 : 0), y + f.alto / 2 - medio, ex + (f.hijo ? 16 : 2), y + f.alto / 2 + medio,
                        alfa(acento, e.hover * a));
            }
            int base = !e.activa ? 0xFF55555C : f.hijo ? 0xFF9A9AA6 : 0xFFB4B4BE;
            int color = mezclar(base, e.peligro ? 0xFFFF7070 : 0xFFFFFFFF, e.hover);
            float tx = ex + (f.hijo ? 26 : 16) + 4 * e.hover;
            float escala = f.hijo ? 0.9f : 1f;
            p.texto(Tr.t(e.texto), tx, y + (f.alto - 8 * escala) / 2f, alfa(color, a), escala, false);
            if (!e.hijos.isEmpty()) {  // flecha: › cerrado, ⌄ abierto (gira con la animación)
                int fx = ANCHO + dx - 16, fy = y + f.alto / 2;
                int c = alfa(0xFF8C8C96, a);
                if (e.apertura < 0.5f) {
                    p.rect(fx, fy - 3, fx + 1, fy + 4, c);
                    p.rect(fx + 1, fy - 2, fx + 2, fy + 3, c);
                    p.rect(fx + 2, fy - 1, fx + 3, fy + 2, c);
                    p.rect(fx + 3, fy, fx + 4, fy + 1, c);
                } else {
                    p.rect(fx - 2, fy - 1, fx + 5, fy, c);
                    p.rect(fx - 1, fy, fx + 4, fy + 1, c);
                    p.rect(fx, fy + 1, fx + 3, fy + 2, c);
                    p.rect(fx + 1, fy + 2, fx + 2, fy + 3, c);
                }
            }
        }
        // pie (si no lo tapan las entradas, en pantallas bajas)
        Fila ultima = filas.isEmpty() ? null : filas.get(filas.size() - 1);
        if (ultima == null || ultima.y + ultima.alto < h - 32) {
            p.texto(pie1, 14 + dx, h - 28, alfa(0xFFB4B4BE, aparecer), 0.85f, false);
            p.texto(pie2, 14 + dx, h - 16, alfa(0xFF6E6E78, aparecer), 0.75f, false);
        }
        if (logoGrande) {
            logoGrande(p, w, h, t);
        }
    }

    private void fondo(Pincel p, int w, int h, float t, boolean dm) {
        if (dm) {
            festivo(p, w, h, t);
            return;
        }
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

    /**
     * Fondo de las festividades: papel picado (o luces) arriba, pétalos, confeti o nieve cayendo, y una fila de
     * adornos abajo (cempasúchiles, banderitas o regalos).
     */
    private void festivo(Pincel p, int w, int h, float t) {
        p.degradado(0, 0, w, h, Temporada.fondoArriba(), Temporada.fondoAbajo());
        float a = suave(t / 1.0f);
        int cx = ANCHO + (w - ANCHO) / 2, cy = h / 2 - 16;
        int r = Math.min(w - ANCHO, h) * 3 / 4;
        p.brillo(cx - r, cy - r * 3 / 4, 2 * r, r * 3 / 2, (0.09f + 0.03f * (float) Math.sin(t * 0.8f)) * a);
        Temporada.petalos(p, ANCHO, 0, w - ANCHO, h, t, a);
        Temporada.papelPicado(p, ANCHO + 4, w - 4, 3, t, a);
        Temporada.papelPicado(p, ANCHO + 11, w - 11, 19, t + 1.7f, a * 0.55f);
        // fila de cempasúchiles al pie, como en una ofrenda
        p.degradado(ANCHO, h - 22, w, h, 0x00000000, alfa(0x55000000 | (Temporada.principal() & 0xFFFFFF), a));
        for (int x = ANCHO + 6, k = 0; x < w - 12; x += 17, k++) {
            int sube = Math.round((float) Math.sin(t * 1.2f + k) * 1.0f);
            Temporada.adorno(p, x, h - 13 - (k % 2) * 4 + sube, 2, a, k);
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
        boolean dm = Temporada.activa();
        String lema = dm ? Tr.t(Temporada.nombre()) + " · NexLauncher" : "PvP · NexLauncher";
        p.texto(lema, cx - p.ancho(lema) / 2f, y + tam + 12 + 9 * escala, alfa(dm ? Temporada.textoTema() : 0xFF8C8C96, a), 1, false);
        if (dm) {  // la figura de la festividad a los lados del logo, flotando a destiempo
            int celda = tam >= 70 ? 3 : 2, lado = 12 * celda;
            int sep = tam / 2 + 22 + lado;
            for (int k = -1; k <= 1; k += 2) {
                float f = (float) Math.sin(t * 1.4f + (k > 0 ? 1.6f : 0)) * 3;
                Temporada.calavera(p, cx + k * sep - lado / 2, Math.round(y + tam / 2f - lado / 2f + f), celda, a);
            }
        }
    }
}
