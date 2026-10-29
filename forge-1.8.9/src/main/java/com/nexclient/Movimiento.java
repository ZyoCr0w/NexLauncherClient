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

    /** Aumento del zoom en este momento: arranca en el de los ajustes y la rueda del ratón lo cambia. */
    public static float zoomObjetivo = 4.0f;
    private static float zoomMostrado = 1.0f;
    private static long tZoom;

    /** Rueda del ratón con el zoom puesto: cada paso acerca o aleja un 20 %. */
    public static void rueda(double pasos) {
        if (pasos == 0) {
            return;
        }
        float f = (float) Math.pow(1.2, Math.signum(pasos));
        zoomObjetivo = Math.max(1.1f, Math.min(50f, zoomObjetivo * f));
    }

    public static boolean hayZoom() {
        return zoomActivo || zoomMostrado > 1.001f;
    }

    /** Campo de visión con el zoom aplicado; el cambio se ve suave (no de golpe) a cualquier FPS. */
    public static double fov(double base) {
        long ahora = System.nanoTime();
        float dt = tZoom == 0 ? 0f : Math.min(0.1f, (ahora - tZoom) / 1e9f);
        tZoom = ahora;
        float objetivo = zoomActivo ? zoomObjetivo : 1.0f;
        zoomMostrado += (objetivo - zoomMostrado) * Math.min(1f, dt * 14f);
        if (Math.abs(zoomMostrado - objetivo) < 0.002f) {
            zoomMostrado = objetivo;
        }
        return base / zoomMostrado;
    }
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
                zoomObjetivo = c.zoomFactor;  // cada vez que aprietas la tecla vuelve al de los ajustes
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
