package com.nexclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Guías del editor del HUD (como en Figma): al arrastrar, el elemento se pega al centro de la pantalla, a los
 * bordes, a los tercios y a los bordes o centros de los demás elementos, y se marcan con líneas. Mismo archivo en
 * 1.8.9 y 1.21.1 (no usa nada de Minecraft).
 */
public final class Guias {
    /** Distancia (en píxeles de la interfaz) a la que se pega. */
    public static final int IMAN = 4;
    public static final int COLOR_CENTRO = 0xFFFF55FF;
    public static final int COLOR_BORDE = 0xFF55FFFF;
    public static final int COLOR_TERCIO = 0xFF5599FF;
    public static final int COLOR_ELEMENTO = 0xFF55FF55;

    public static final class Linea {
        public final boolean vertical;
        public final int pos, desde, hasta, color;

        Linea(boolean vertical, int pos, int desde, int hasta, int color) {
            this.vertical = vertical;
            this.pos = pos;
            this.desde = desde;
            this.hasta = hasta;
            this.color = color;
        }
    }

    public static final class Resultado {
        public int x, y;
        public final List<Linea> lineas = new ArrayList<Linea>();
    }

    private static final class Destino {
        final int valor, color, desde, hasta;

        Destino(int valor, int color, int desde, int hasta) {
            this.valor = valor;
            this.color = color;
            this.desde = desde;
            this.hasta = hasta;
        }
    }

    private Guias() {
    }

    /**
     * x, y: donde quedaría el elemento (w × h) sin ayuda. otros: {x, y, w, h} de los demás elementos visibles.
     * iman=false (Shift): se mueve libre, sin pegarse. rejilla > 0: si no hay guía cerca, se pega a la cuadrícula.
     */
    public static Resultado ajustar(int x, int y, int w, int h, int sw, int sh, List<int[]> otros, boolean iman,
                                    int rejilla) {
        Resultado r = new Resultado();
        x = Math.max(0, Math.min(x, sw - w));
        y = Math.max(0, Math.min(y, sh - h));
        r.x = x;
        r.y = y;
        if (!iman) {
            return r;
        }
        // eje X: destinos verticales
        List<Destino> vx = new ArrayList<Destino>();
        vx.add(new Destino(0, COLOR_BORDE, 0, sh));
        vx.add(new Destino(sw, COLOR_BORDE, 0, sh));
        vx.add(new Destino(sw / 2, COLOR_CENTRO, 0, sh));
        vx.add(new Destino(sw / 3, COLOR_TERCIO, 0, sh));
        vx.add(new Destino(sw * 2 / 3, COLOR_TERCIO, 0, sh));
        List<Destino> vy = new ArrayList<Destino>();
        vy.add(new Destino(0, COLOR_BORDE, 0, sw));
        vy.add(new Destino(sh, COLOR_BORDE, 0, sw));
        vy.add(new Destino(sh / 2, COLOR_CENTRO, 0, sw));
        vy.add(new Destino(sh / 3, COLOR_TERCIO, 0, sw));
        vy.add(new Destino(sh * 2 / 3, COLOR_TERCIO, 0, sw));
        for (int[] o : otros) {
            int desdeY = Math.min(o[1], y), hastaY = Math.max(o[1] + o[3], y + h);
            int desdeX = Math.min(o[0], x), hastaX = Math.max(o[0] + o[2], x + w);
            for (int v : new int[]{o[0], o[0] + o[2] / 2, o[0] + o[2]}) {
                vx.add(new Destino(v, COLOR_ELEMENTO, desdeY, hastaY));
            }
            for (int v : new int[]{o[1], o[1] + o[3] / 2, o[1] + o[3]}) {
                vy.add(new Destino(v, COLOR_ELEMENTO, desdeX, hastaX));
            }
        }
        int[] px = {0, w / 2, w};   // borde izquierdo, centro y borde derecho del elemento
        int[] py = {0, h / 2, h};
        int mejorX = Integer.MAX_VALUE, nx = x;
        for (Destino d : vx) {
            for (int p : px) {
                int dif = d.valor - (x + p);
                if (Math.abs(dif) <= IMAN && Math.abs(dif) < Math.abs(mejorX)) {
                    mejorX = dif;
                    nx = x + dif;
                }
            }
        }
        int mejorY = Integer.MAX_VALUE, ny = y;
        for (Destino d : vy) {
            for (int p : py) {
                int dif = d.valor - (y + p);
                if (Math.abs(dif) <= IMAN && Math.abs(dif) < Math.abs(mejorY)) {
                    mejorY = dif;
                    ny = y + dif;
                }
            }
        }
        if (mejorX == Integer.MAX_VALUE && rejilla > 1) {
            nx = Math.round(x / (float) rejilla) * rejilla;
        }
        if (mejorY == Integer.MAX_VALUE && rejilla > 1) {
            ny = Math.round(y / (float) rejilla) * rejilla;
        }
        r.x = Math.max(0, Math.min(nx, sw - w));
        r.y = Math.max(0, Math.min(ny, sh - h));
        // líneas de todas las guías que coinciden con la posición final (puede ser más de una a la vez)
        for (Destino d : vx) {
            for (int p : px) {
                if (d.valor == r.x + p) {
                    r.lineas.add(new Linea(true, Math.min(d.valor, sw - 1), d.desde, d.hasta, d.color));
                }
            }
        }
        for (Destino d : vy) {
            for (int p : py) {
                if (d.valor == r.y + p) {
                    r.lineas.add(new Linea(false, Math.min(d.valor, sh - 1), d.desde, d.hasta, d.color));
                }
            }
        }
        return r;
    }

    /**
     * Ancla del elemento según dónde quedó: a la izquierda, al centro o a la derecha (y arriba, al medio o abajo).
     * Así, al cambiar la resolución o la escala de la interfaz, sigue pegado al mismo lugar.
     * Devuelve {anclaH, anclaV, distanciaX, distanciaY}.
     */
    public static int[] anclar(int x, int y, int w, int h, int sw, int sh) {
        int cx = x + w / 2, cy = y + h / 2;
        int ah = cx < sw / 3 ? 0 : cx > sw * 2 / 3 ? 2 : 1;
        int av = cy < sh / 3 ? 0 : cy > sh * 2 / 3 ? 2 : 1;
        int ox = ah == 0 ? x : ah == 2 ? sw - (x + w) : cx - sw / 2;
        int oy = av == 0 ? y : av == 2 ? sh - (y + h) : cy - sh / 2;
        return new int[]{ah, av, ox, oy};
    }

    /** Posición {x, y} a partir del ancla guardada. */
    public static int[] desanclar(int[] a, int w, int h, int sw, int sh) {
        int x = a[0] == 0 ? a[2] : a[0] == 2 ? sw - w - a[2] : sw / 2 + a[2] - w / 2;
        int y = a[1] == 0 ? a[3] : a[1] == 2 ? sh - h - a[3] : sh / 2 + a[3] - h / 2;
        return new int[]{Math.max(0, Math.min(x, sw - w)), Math.max(0, Math.min(y, sh - h))};
    }
}
