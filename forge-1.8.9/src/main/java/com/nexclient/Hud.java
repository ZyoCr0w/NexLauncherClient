package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/** Dibuja las teclas en pantalla, el contador de CPS y el de FPS. */
public final class Hud {
    public static final int TECLAS = 0, CPS = 1, FPS = 2;
    public static final String[] NOMBRES = {"Teclas", "CPS", "FPS"};
    private static final float[] BRILLO = new float[7];
    private static long ultimo = System.nanoTime();

    private Hud() {
    }

    public static void dibujar(boolean editor) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!editor && (mc.gameSettings.hideGUI || mc.gameSettings.showDebugInfo || mc.thePlayer == null)) {
            return;
        }
        long ahora = System.nanoTime();
        float dt = Math.min(0.1f, (ahora - ultimo) / 1.0e9f);
        ultimo = ahora;
        Config c = Config.get();
        GlStateManager.pushMatrix();
        if (c.teclas || editor) {
            teclas(mc, c, dt);
        }
        if (c.cps || editor) {
            caja(mc.fontRendererObj, rect(CPS, mc), Cps.izquierdo() + " | " + Cps.derecho() + " CPS");
        }
        if (c.fps || editor) {
            caja(mc.fontRendererObj, rect(FPS, mc), Minecraft.getDebugFPS() + " FPS");
        }
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
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

    public static int[] rect(int cual, Minecraft mc) {
        Config c = Config.get();
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
        int x, y, w, h;
        if (cual == TECLAS) {
            x = c.teclasX;
            y = c.teclasY;
            w = Math.round(70 * c.teclasEscala);
            h = Math.round(altoBase(c) * c.teclasEscala);
        } else {
            String texto = cual == CPS ? "88 | 88 CPS" : "888 FPS";
            x = cual == CPS ? c.cpsX : c.fpsX;
            y = cual == CPS ? c.cpsY : c.fpsY;
            w = mc.fontRendererObj.getStringWidth(texto) + 8;
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

    private static void caja(FontRenderer f, int[] r, String texto) {
        Gui.drawRect(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x66000000);
        f.drawString(texto, r[0] + (r[2] - f.getStringWidth(texto)) / 2f, r[1] + 3, 0xFFFFFFFF, true);
    }

    private static void teclas(Minecraft mc, Config c, float dt) {
        int[] r = rect(TECLAS, mc);
        GameSettings o = mc.gameSettings;
        KeyBinding[] k = {o.keyBindForward, o.keyBindLeft, o.keyBindBack, o.keyBindRight, o.keyBindAttack,
                o.keyBindUseItem, o.keyBindJump};
        for (int i = 0; i < k.length; i++) {
            float objetivo = k[i].isKeyDown() ? 1f : 0f;
            float vel = objetivo > BRILLO[i] ? 28f : 9f;
            BRILLO[i] += (objetivo - BRILLO[i]) * Math.min(1f, dt * vel);
        }
        FontRenderer f = mc.fontRendererObj;
        GlStateManager.pushMatrix();
        GlStateManager.translate(r[0], r[1], 0);
        GlStateManager.scale(c.teclasEscala, c.teclasEscala, 1f);
        tecla(f, 24, 0, 22, 22, nombre(k[0]), null, BRILLO[0]);
        tecla(f, 0, 24, 22, 22, nombre(k[1]), null, BRILLO[1]);
        tecla(f, 24, 24, 22, 22, nombre(k[2]), null, BRILLO[2]);
        tecla(f, 48, 24, 22, 22, nombre(k[3]), null, BRILLO[3]);
        int y = 48;
        if (c.teclasRaton) {
            tecla(f, 0, y, 34, 22, "LMB", c.teclasCps ? Cps.izquierdo() + " CPS" : null, BRILLO[4]);
            tecla(f, 36, y, 34, 22, "RMB", c.teclasCps ? Cps.derecho() + " CPS" : null, BRILLO[5]);
            y += 24;
        }
        if (c.teclasEspacio) {
            Gui.drawRect(0, y, 70, y + 10, mezclar(0x66000000, 0xCCFFFFFF, BRILLO[6]));
            Gui.drawRect(22, y + 4, 48, y + 6, mezclar(0xFFFFFFFF, 0xFF000000, BRILLO[6]));
        }
        GlStateManager.popMatrix();
    }

    private static String nombre(KeyBinding k) {
        String s = GameSettings.getKeyDisplayString(k.getKeyCode());
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    private static void tecla(FontRenderer f, int x, int y, int w, int h, String texto, String sub, float b) {
        Gui.drawRect(x, y, x + w, y + h, mezclar(0x66000000, 0xCCFFFFFF, b));
        int color = mezclar(0xFFFFFFFF, 0xFF000000, b);
        boolean sombra = b < 0.5f;
        if (sub == null) {
            f.drawString(texto, x + (w - f.getStringWidth(texto)) / 2f, y + (h - 8) / 2f, color, sombra);
        } else {
            f.drawString(texto, x + (w - f.getStringWidth(texto)) / 2f, y + 3, color, sombra);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + w / 2f, y + 13, 0);
            GlStateManager.scale(0.6f, 0.6f, 1f);
            f.drawString(sub, -f.getStringWidth(sub) / 2f, 0, color, sombra);
            GlStateManager.popMatrix();
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
