package com.nexclient;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Vista libre (freelook): la cámara pasa a tercera persona y gira con el ratón alrededor de tu
 * personaje sin cambiar hacia dónde miras ni hacia dónde caminas. Tecla R por defecto.
 */
public final class VistaLibre {
    public static KeyMapping tecla;
    public static boolean activa;
    public static float yaw, pitch;
    private static CameraType previa;
    private static boolean alternada;

    private VistaLibre() {
    }

    public static void tick(Minecraft mc) {
        Config c = Config.get();
        boolean quiere = false;
        if (tecla != null) {
            if (c.vistaLibre == 2) {
                while (tecla.consumeClick()) {
                    alternada = !alternada;
                }
                quiere = alternada;
            } else {
                while (tecla.consumeClick()) {
                    // en modo «mantener» solo importa si está apretada
                }
                alternada = false;
                quiere = c.vistaLibre == 1 && mc.screen == null && Movimiento.fisico(mc, tecla);
            }
        }
        if (mc.player == null || mc.level == null) {
            quiere = false;
            alternada = false;
        }
        if (quiere && !activa) {
            activa = true;
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
            previa = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!quiere && activa) {
            activa = false;
            if (previa != null) {
                mc.options.setCameraType(previa);
            }
        }
    }

    /** El ratón mueve la cámara libre en vez de al jugador (mismos 0.15 que usa el juego). */
    public static void girar(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }
}
