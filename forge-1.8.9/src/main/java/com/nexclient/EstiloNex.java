package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Estilo NexClient para toda la interfaz (1.8.9): botones y deslizadores oscuros, planos y transparentes, y un fondo
 * oscuro en lugar de la tierra. Se dibuja con rectángulos y texto: el paquete de texturas no lo cambia.
 */
public final class EstiloNex {
    private static final Map<GuiButton, float[]> HOVER = new WeakHashMap<GuiButton, float[]>();
    private static final Map<Class<?>, Field> VALOR = new WeakHashMap<Class<?>, Field>();
    private static final Field SIN_VALOR;

    static {
        Field f = null;
        try {
            f = EstiloNex.class.getDeclaredField("SIN_VALOR");
        } catch (NoSuchFieldException ignorada) {
            // imposible
        }
        SIN_VALOR = f;
    }

    private EstiloNex() {
    }

    public static boolean activo() {
        return Config.get().menuNex;
    }

    /** Animación suave del resaltado al pasar el ratón (0..1). */
    private static float hover(GuiButton b, boolean encima) {
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

    private static int mezclar(int c1, int c2, float k) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int a = (c1 >>> s) & 0xFF, b = (c2 >>> s) & 0xFF;
            r |= Math.round(a + (b - a) * k) << s;
        }
        return r;
    }

    /** Valor 0..1 de un deslizador (de Minecraft, de Forge o de OptiFine), o -1 si no lo es. */
    private static double valor(GuiButton b) {
        Class<?> c = b.getClass();
        Field f = VALOR.get(c);
        if (f == null) {
            f = SIN_VALOR;
            for (Class<?> k = c; k != null && k != GuiButton.class && f == SIN_VALOR; k = k.getSuperclass()) {
                for (String n : new String[]{"sliderValue", "field_146134_p"}) {
                    try {
                        Field x = k.getDeclaredField(n);
                        if (x.getType() == float.class || x.getType() == double.class) {
                            x.setAccessible(true);
                            f = x;
                            break;
                        }
                    } catch (NoSuchFieldException ignorada) {
                        // sigue buscando
                    }
                }
            }
            VALOR.put(c, f);
        }
        if (f == SIN_VALOR) {
            return -1;
        }
        try {
            return Math.max(0, Math.min(1, ((Number) f.get(b)).doubleValue()));
        } catch (IllegalAccessException e) {
            return -1;
        }
    }

    /** Fondo, borde y deslizador del botón. Devuelve el color del texto. */
    public static int fondoBoton(GuiButton b, int x, int y, int w, int h, boolean encima, boolean activo) {
        float k = hover(b, encima && activo);
        int fondo = activo ? mezclar(0x99101014, 0xC0282830, k) : 0x60101014;
        int borde = activo ? mezclar(0x24FFFFFF, 0x80FFFFFF, k) : 0x14FFFFFF;
        Gui.drawRect(x, y, x + w, y + h, fondo);
        Gui.drawRect(x, y, x + w, y + 1, borde);
        Gui.drawRect(x, y + h - 1, x + w, y + h, borde);
        Gui.drawRect(x, y + 1, x + 1, y + h - 1, borde);
        Gui.drawRect(x + w - 1, y + 1, x + w, y + h - 1, borde);
        if (k > 0.02f) {  // barrita a la izquierda que crece
            int m = Math.round((h / 2f - 3) * k);
            Gui.drawRect(x + 1, y + h / 2 - m, x + 3, y + h / 2 + m, (Math.round(255 * k) << 24) | 0xFFFFFF);
        }
        return !activo ? 0xFF6E6E78 : mezclar(0xFFD8D8DE, 0xFFFFFFFF, k);
    }

    /**
     * Dibuja un botón entero. mouseDragged del juego (que mueve los deslizadores al arrastrar) se llama igual, pero
     * con el color apagado: así su perilla con la textura del paquete no se ve y se dibuja la nuestra.
     */
    public static void boton(GuiButton b, Minecraft mc, int mx, int my) {
        int x = b.xPosition, y = b.yPosition, w = b.width, h = b.height;
        boolean encima = mx >= x && my >= y && mx < x + w && my < y + h;
        GlStateManager.enableBlend();
        int color = fondoBoton(b, x, y, w, h, encima, b.enabled);
        GlStateManager.colorMask(false, false, false, false);
        try {
            ((ArrastreBoton) b).nexclient$arrastrar(mc, mx, my);
        } finally {
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.color(1, 1, 1, 1);
        }
        double v = valor(b);
        if (v >= 0) {
            int px = x + 2 + (int) Math.round(v * (w - 8));
            Gui.drawRect(x + 1, y + 1, px, y + h - 1, 0x20FFFFFF);
            Gui.drawRect(px, y + 2, px + 4, y + h - 2, encima ? 0xFFFFFFFF : 0xFFC8C8D0);
        }
        if (b.packedFGColour != 0) {
            color = 0xFF000000 | b.packedFGColour;
        }
        FontRenderer f = mc.fontRendererObj;
        String s = Tr.t(b.displayString);
        int max = w - 6;
        if (f.getStringWidth(s) > max) {  // que no se salga del botón
            s = f.trimStringToWidth(s, max - f.getStringWidth("…")) + "…";
        }
        f.drawStringWithShadow(s, x + w / 2f - f.getStringWidth(s) / 2f, y + (h - 8) / 2f, color);
    }

    /** Fondo de las pantallas sin mundo detrás (en lugar de la tierra). */
    public static void fondo(int w, int h) {
        dibujarDegradado(0, 0, w, h, 0xFF101016, 0xFF040406);
    }

    /** Las franjas de arriba y abajo de las listas (mundos, servidores, paquetes). */
    public static void franja(int x1, int y1, int x2, int y2) {
        Gui.drawRect(x1, y1, x2, y2, 0xE00A0A0E);
    }

    private static void dibujarDegradado(int x1, int y1, int x2, int y2, int arriba, int abajo) {
        GRADIENTE.degradado(x1, y1, x2, y2, arriba, abajo);
    }

    private static final Degradado GRADIENTE = new Degradado();

    private static final class Degradado extends Gui {
        void degradado(int x1, int y1, int x2, int y2, int a, int b) {
            drawGradientRect(x1, y1, x2, y2, a, b);
        }
    }
}
