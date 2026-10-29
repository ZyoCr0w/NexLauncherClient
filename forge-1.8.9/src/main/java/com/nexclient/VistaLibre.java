package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * Vista libre (freelook): la cámara pasa a tercera persona y gira con el ratón alrededor de tu
 * personaje sin cambiar hacia dónde miras ni hacia dónde caminas. Tecla R por defecto.
 */
public final class VistaLibre {
    public static KeyBinding tecla;
    public static boolean activa;
    public static float yaw, pitch;
    private static int previa;
    private static boolean alternada;

    private VistaLibre() {
    }

    public static void tick(Minecraft mc) {
        Config c = Config.get();
        boolean quiere = false;
        if (tecla != null) {
            if (c.vistaLibre == 2) {
                while (tecla.isPressed()) {
                    alternada = !alternada;
                }
                quiere = alternada;
            } else {
                while (tecla.isPressed()) {
                    // en modo «mantener» solo importa si está apretada
                }
                alternada = false;
                quiere = c.vistaLibre == 1 && mc.currentScreen == null && tecla.isKeyDown();
            }
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            quiere = false;
            alternada = false;
        }
        if (quiere && !activa) {
            activa = true;
            yaw = mc.thePlayer.rotationYaw;
            pitch = mc.thePlayer.rotationPitch;
            previa = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1;
        } else if (!quiere && activa) {
            activa = false;
            mc.gameSettings.thirdPersonView = previa;
        }
    }

    /** El ratón mueve la cámara libre en vez de al jugador (como Entity.setAngles). */
    public static void girar(float dx, float dy) {
        yaw += dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch - dy * 0.15f));
    }
}
