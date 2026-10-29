package com.nexclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import com.nexclient.mixin.OptionInstanceAccessor;
import org.lwjgl.glfw.GLFW;

/** Toggle sprint / sneak, zoom y fullbright. */
public final class Movimiento {
    public static KeyMapping teclaZoom;
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
    private static double gammaPrevio = 0.5;

    private Movimiento() {
    }

    private static boolean fisico(Minecraft mc, KeyMapping k) {
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(k);
        long w = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(w, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return key.getValue() >= 0 && InputConstants.isKeyDown(w, key.getValue());
    }

    /** Se llama al inicio de cada tick, antes de que el juego lea las teclas de movimiento. */
    public static void tick(Minecraft mc) {
        Config c = Config.get();
        Options o = mc.options;
        boolean libre = mc.screen == null;
        // toggle sprint: la tecla de correr alterna en vez de mantener
        boolean s = libre && fisico(mc, o.keySprint);
        if (c.toggleSprint) {
            if (s && !prevSprint) {
                sprintFijo = !sprintFijo;
            }
            o.keySprint.setDown(sprintFijo || s);
        } else if (sprintFijo) {
            sprintFijo = false;
            o.keySprint.setDown(s);
        }
        prevSprint = s;
        boolean k = libre && fisico(mc, o.keyShift);
        if (c.toggleSneak) {
            if (k && !prevSneak) {
                sneakFijo = !sneakFijo;
            }
            o.keyShift.setDown(sneakFijo || k);
        } else if (sneakFijo) {
            sneakFijo = false;
            o.keyShift.setDown(k);
        }
        prevSneak = k;
        // zoom (mantener la tecla)
        boolean z = c.zoom && libre && teclaZoom != null && teclaZoom.isDown();
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
        // fullbright
        OptionInstance<Double> gamma = o.gamma();
        if (c.fullbright) {
            if (!fbActivo) {
                gammaPrevio = gamma.get();
                fbActivo = true;
            }
            ((OptionInstanceAccessor) (Object) gamma).nexclient$setValue(15.0);
        } else if (fbActivo) {
            ((OptionInstanceAccessor) (Object) gamma).nexclient$setValue(gammaPrevio);
            fbActivo = false;
        }
    }

    public static String textoEstado(Minecraft mc) {
        if (mc.player == null) {
            return null;
        }
        if (sneakFijo || (mc.player.isShiftKeyDown() && Config.get().toggleSneak)) {
            return "[Agachado" + (sneakFijo ? " (fijo)]" : "]");
        }
        if (sprintFijo) {
            return "[Sprint (fijo)]";
        }
        if (mc.player.isSprinting()) {
            return "[Corriendo]";
        }
        return null;
    }
}
