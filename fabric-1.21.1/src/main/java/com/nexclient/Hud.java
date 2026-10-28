package com.nexclient;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Dibuja las teclas en pantalla, el contador de CPS y el de FPS. */
public final class Hud {
    public static final int TECLAS = 0, CPS = 1, FPS = 2;
    public static final String[] NOMBRES = {"Teclas", "CPS", "FPS"};

    /** Brillo animado de cada tecla: W A S D LMB RMB Espacio. */
    private static final float[] BRILLO = new float[7];
    private static long ultimo = System.nanoTime();

    private Hud() {
    }

    public static void dibujar(GuiGraphics g, boolean editor) {
        Minecraft mc = Minecraft.getInstance();
        if (!editor && (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || mc.player == null)) {
            return;
        }
        long ahora = System.nanoTime();
        float dt = Math.min(0.1f, (ahora - ultimo) / 1.0e9f);
        ultimo = ahora;
        Config c = Config.get();
        if (c.teclas || editor) {
            teclas(g, mc, c, dt);
        }
        if (c.cps || editor) {
            int[] r = rect(CPS, mc);
            caja(g, mc.font, r, Cps.izquierdo() + " | " + Cps.derecho() + " CPS");
        }
        if (c.fps || editor) {
            int[] r = rect(FPS, mc);
            caja(g, mc.font, r, mc.getFps() + " FPS");
        }
    }

    // ---- posiciones (en coordenadas de la interfaz)
    public static int anchoBase() {
        return 70;
    }

    public static int altoBase(Config c) {
        int h = 46;
        if (c.teclasRaton) {
            h += 24;
        }
        if (c.teclasEspacio) {
            h += 12;
        }
        return h;
    }

    /** {x, y, ancho, alto} del elemento, sin salirse de la pantalla. */
    public static int[] rect(int cual, Minecraft mc) {
        Config c = Config.get();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int x, y, w, h;
        if (cual == TECLAS) {
            x = c.teclasX;
            y = c.teclasY;
            w = Math.round(anchoBase() * c.teclasEscala);
            h = Math.round(altoBase(c) * c.teclasEscala);
        } else {
            String texto = cual == CPS ? "88 | 88 CPS" : "888 FPS";
            x = cual == CPS ? c.cpsX : c.fpsX;
            y = cual == CPS ? c.cpsY : c.fpsY;
            w = mc.font.width(texto) + 8;
            h = 13;
        }
        x = Math.max(0, Math.min(x, sw - w));
        y = Math.max(0, Math.min(y, sh - h));
        return new int[]{x, y, w, h};
    }

    public static void mover(int cual, int x, int y) {
        Config c = Config.get();
        if (cual == TECLAS) {
            c.teclasX = x;
            c.teclasY = y;
        } else if (cual == CPS) {
            c.cpsX = x;
            c.cpsY = y;
        } else {
            c.fpsX = x;
            c.fpsY = y;
        }
    }

    // ---- dibujo
    private static void caja(GuiGraphics g, Font f, int[] r, String texto) {
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x66000000);
        g.drawString(f, texto, r[0] + (r[2] - f.width(texto)) / 2, r[1] + 3, 0xFFFFFFFF, true);
    }

    private static void teclas(GuiGraphics g, Minecraft mc, Config c, float dt) {
        int[] r = rect(TECLAS, mc);
        KeyMapping[] k = {mc.options.keyUp, mc.options.keyLeft, mc.options.keyDown, mc.options.keyRight,
                mc.options.keyAttack, mc.options.keyUse, mc.options.keyJump};
        for (int i = 0; i < k.length; i++) {
            float objetivo = k[i].isDown() ? 1f : 0f;
            // se ilumina rápido y se apaga suave, igual a cualquier FPS
            float vel = objetivo > BRILLO[i] ? 28f : 9f;
            BRILLO[i] += (objetivo - BRILLO[i]) * Math.min(1f, dt * vel);
        }
        g.pose().pushPose();
        g.pose().translate(r[0], r[1], 0);
        g.pose().scale(c.teclasEscala, c.teclasEscala, 1f);
        Font f = mc.font;
        tecla(g, f, 24, 0, 22, 22, nombre(k[0]), null, BRILLO[0]);
        tecla(g, f, 0, 24, 22, 22, nombre(k[1]), null, BRILLO[1]);
        tecla(g, f, 24, 24, 22, 22, nombre(k[2]), null, BRILLO[2]);
        tecla(g, f, 48, 24, 22, 22, nombre(k[3]), null, BRILLO[3]);
        int y = 48;
        if (c.teclasRaton) {
            tecla(g, f, 0, y, 34, 22, "LMB", c.teclasCps ? Cps.izquierdo() + " CPS" : null, BRILLO[4]);
            tecla(g, f, 36, y, 34, 22, "RMB", c.teclasCps ? Cps.derecho() + " CPS" : null, BRILLO[5]);
            y += 24;
        }
        if (c.teclasEspacio) {
            int fondo = mezclar(0x66000000, 0xCCFFFFFF, BRILLO[6]);
            g.fill(0, y, 70, y + 10, fondo);
            g.fill(22, y + 4, 48, y + 6, mezclar(0xFFFFFFFF, 0xFF000000, BRILLO[6]));
        }
        g.pose().popPose();
    }

    private static String nombre(KeyMapping k) {
        String s = k.getTranslatedKeyMessage().getString();
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    private static void tecla(GuiGraphics g, Font f, int x, int y, int w, int h, String texto, String sub, float b) {
        g.fill(x, y, x + w, y + h, mezclar(0x66000000, 0xCCFFFFFF, b));
        int color = mezclar(0xFFFFFFFF, 0xFF000000, b);
        boolean sombra = b < 0.5f;
        if (sub == null) {
            g.drawString(f, texto, x + (w - f.width(texto)) / 2, y + (h - 8) / 2, color, sombra);
        } else {
            g.drawString(f, texto, x + (w - f.width(texto)) / 2, y + 3, color, sombra);
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, y + 13, 0);
            g.pose().scale(0.6f, 0.6f, 1f);
            g.drawString(f, sub, -f.width(sub) / 2, 0, color, sombra);
            g.pose().popPose();
        }
    }

    public static int mezclar(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }
}
