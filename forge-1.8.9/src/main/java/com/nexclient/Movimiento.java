package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Toggle sprint / sneak, zoom y fullbright. */
public final class Movimiento {
    public static KeyBinding teclaZoom;
    public static boolean zoomActivo;
    private static boolean sprintFijo, sneakFijo, prevSprint, prevSneak, suavePrevio, fbActivo;
    private static float gammaPrevio = 0.5f;

    private Movimiento() {
    }

    private static boolean fisico(KeyBinding k) {
        int c = k.getKeyCode();
        if (c < 0) {
            return Mouse.isButtonDown(c + 100);
        }
        return c > 0 && Keyboard.isKeyDown(c);
    }

    public static void tick(Minecraft mc) {
        Config c = Config.get();
        GameSettings o = mc.gameSettings;
        boolean libre = mc.currentScreen == null;
        boolean s = libre && fisico(o.keyBindSprint);
        if (c.toggleSprint) {
            if (s && !prevSprint) {
                sprintFijo = !sprintFijo;
            }
            KeyBinding.setKeyBindState(o.keyBindSprint.getKeyCode(), sprintFijo || s);
        } else if (sprintFijo) {
            sprintFijo = false;
            KeyBinding.setKeyBindState(o.keyBindSprint.getKeyCode(), s);
        }
        prevSprint = s;
        boolean k = libre && fisico(o.keyBindSneak);
        if (c.toggleSneak) {
            if (k && !prevSneak) {
                sneakFijo = !sneakFijo;
            }
            KeyBinding.setKeyBindState(o.keyBindSneak.getKeyCode(), sneakFijo || k);
        } else if (sneakFijo) {
            sneakFijo = false;
            KeyBinding.setKeyBindState(o.keyBindSneak.getKeyCode(), k);
        }
        prevSneak = k;
        boolean z = c.zoom && libre && teclaZoom != null && fisico(teclaZoom);
        if (z != zoomActivo) {
            if (z) {
                suavePrevio = o.smoothCamera;
                if (c.zoomSuave) {
                    o.smoothCamera = true;
                }
            } else {
                o.smoothCamera = suavePrevio;
            }
            zoomActivo = z;
        }
        if (c.fullbright) {
            if (!fbActivo) {
                gammaPrevio = o.gammaSetting;
                fbActivo = true;
            }
            o.gammaSetting = 100f;
        } else if (fbActivo) {
            o.gammaSetting = gammaPrevio;
            fbActivo = false;
        }
    }

    public static String textoEstado(Minecraft mc) {
        if (mc.thePlayer == null) {
            return null;
        }
        if (sneakFijo) {
            return "[Agachado (fijo)]";
        }
        if (mc.thePlayer.isSneaking()) {
            return "[Agachado]";
        }
        if (sprintFijo) {
            return "[Sprint (fijo)]";
        }
        if (mc.thePlayer.isSprinting()) {
            return "[Corriendo]";
        }
        return null;
    }
}
