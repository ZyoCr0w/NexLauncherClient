package com.nexclient;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Estilo NexClient para toda la interfaz (1.21.1): botones y deslizadores oscuros, planos y transparentes, y fondo
 * oscuro en lugar del panorama y la tierra. Se dibuja con rectángulos y texto: el paquete de texturas no lo cambia.
 */
public final class EstiloNex {
    private static final Map<Object, float[]> HOVER = new WeakHashMap<>();

    private EstiloNex() {
    }

    public static boolean activo() {
        return Config.get().menuNex;
    }

    private static float hover(Object b, boolean encima) {
        float[] h = HOVER.get(b);
        long ahora = System.nanoTime();
        if (h == null) {
            h = new float[]{encima ? 1 : 0, ahora};
            HOVER.put(b, h);
        }
        float dt = Math.min(0.1f, (ahora - (long) h[1]) / 1e9f);
        h[1] = ahora;
        h[0] += ((encima ? 1 : 0) - h[0]) * Math.min(1, dt * 14);
        return h[0];
    }

    static int mezclar(int c1, int c2, float k) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int a = (c1 >>> s) & 0xFF, b = (c2 >>> s) & 0xFF;
            r |= Math.round(a + (b - a) * k) << s;
        }
        return r;
    }

    private static int alfa(int argb, float a) {
        return (Math.round(((argb >>> 24) & 0xFF) * a) << 24) | (argb & 0xFFFFFF);
    }

    /** Fondo, borde y (si valor >= 0) deslizador. Devuelve el color del texto. */
    public static int boton(GuiGraphics g, Object b, int x, int y, int w, int h, boolean encima, boolean activo,
                            float transparencia, double valor) {
        float k = hover(b, encima && activo);
        int fondo = activo ? mezclar(0x99101014, 0xC0282830, k) : 0x60101014;
        int borde = activo ? mezclar(0x24FFFFFF, 0x80FFFFFF, k) : 0x14FFFFFF;
        g.fill(x, y, x + w, y + h, alfa(fondo, transparencia));
        g.renderOutline(x, y, w, h, alfa(borde, transparencia));
        if (valor >= 0) {
            int px = x + 2 + (int) Math.round(valor * (w - 8));
            g.fill(x + 1, y + 1, px, y + h - 1, alfa(0x20FFFFFF, transparencia));
            g.fill(px, y + 2, px + 4, y + h - 2, alfa(encima ? 0xFFFFFFFF : 0xFFC8C8D0, transparencia));
        } else if (k > 0.02f) {  // barrita a la izquierda que crece
            int m = Math.round((h / 2f - 3) * k);
            g.fill(x + 1, y + h / 2 - m, x + 3, y + h / 2 + m, alfa((Math.round(255 * k) << 24) | 0xFFFFFF, transparencia));
        }
        int texto = !activo ? 0xFF6E6E78 : mezclar(0xFFD8D8DE, 0xFFFFFFFF, k);
        return alfa(texto, Math.max(transparencia, 0.05f));
    }

    /** Fondo de las pantallas: oscuro sin mundo detrás; con mundo, solo oscurece (sin desenfoque). */
    public static void fondo(GuiGraphics g, int w, int h, boolean conMundo) {
        if (conMundo) {
            g.fillGradient(0, 0, w, h, 0x88000000, 0xA8000000);
        } else {
            g.fillGradient(0, 0, w, h, 0xFF101016, 0xFF040406);
        }
    }
}
