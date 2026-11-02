package com.nexclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

/**
 * Ranuras de la barra puestas en un botón del ratón (los laterales, por ejemplo).
 *
 * Además del clic que llega como evento, en cada cuadro se mira si el botón está apretado (y en Windows se le
 * pregunta directo al sistema, que lo ve aunque se pierda el mensaje): en cuanto se aprieta, cambias de objeto,
 * también caminando o agachado.
 */
public final class RanuraRaton {
    private static final boolean[] antes = new boolean[8];
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    private static final int[] VK = {0x01, 0x02, 0x04, 0x05, 0x06};
    private static Object getAsyncKeyState;
    private static Method invocarInt;
    private static boolean sinSistema;
    private static boolean avisoEvento, avisoSondeo;

    private RanuraRaton() {
    }

    public static void clic(Minecraft mc, int boton) {
        if (elegir(mc, boton) && !avisoEvento) {
            avisoEvento = true;
            System.out.println("[NexClient] Ranura por botón del ratón " + boton + " (evento)");
        }
    }

    public static void sondear(Minecraft mc) {
        boolean jugando = mc.player != null && mc.screen == null && mc.isWindowActive() && mc.mouseHandler.isMouseGrabbed();
        for (KeyMapping k : mc.options.keyHotbarSlots) {
            InputConstants.Key tecla = KeyBindingHelper.getBoundKeyOf(k);
            int b = tecla.getValue();
            if (tecla.getType() != InputConstants.Type.MOUSE || b < 0 || b >= antes.length) {
                continue;
            }
            boolean ahora = jugando && apretado(mc, b);
            if (ahora && !antes[b] && elegir(mc, b) && !avisoSondeo) {
                avisoSondeo = true;
                System.out.println("[NexClient] Ranura por botón del ratón " + b + " (sondeo)");
            }
            antes[b] = ahora;
        }
    }

    private static boolean elegir(Minecraft mc, int boton) {
        if (mc.player == null || mc.screen != null || mc.player.isSpectator()) {
            return false;
        }
        KeyMapping[] barra = mc.options.keyHotbarSlots;
        for (int i = 0; i < barra.length; i++) {
            InputConstants.Key k = KeyBindingHelper.getBoundKeyOf(barra[i]);
            if (k.getType() == InputConstants.Type.MOUSE && k.getValue() == boton) {
                // en creativo, C/X + número guarda o carga una barra: eso lo hace el juego
                if (!(mc.player.isCreative() && (mc.options.keySaveHotbarActivator.isDown()
                        || mc.options.keyLoadHotbarActivator.isDown()))) {
                    mc.player.getInventory().selected = i;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean apretado(Minecraft mc, int b) {
        if (GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), b) == GLFW.GLFW_PRESS) {
            return true;
        }
        return b < VK.length && sistema(VK[b]);
    }

    private static boolean sistema(int vk) {
        if (!WINDOWS || sinSistema) {
            return false;
        }
        try {
            if (getAsyncKeyState == null) {
                Class<?> f = Class.forName("com.sun.jna.Function");
                getAsyncKeyState = f.getMethod("getFunction", String.class, String.class).invoke(null, "user32", "GetAsyncKeyState");
                invocarInt = f.getMethod("invokeInt", Object[].class);
            }
            int r = (Integer) invocarInt.invoke(getAsyncKeyState, new Object[]{new Object[]{vk}});
            return (r & 0x8000) != 0;
        } catch (Throwable e) {
            sinSistema = true;
            System.err.println("[NexClient] Sin lectura directa del ratón: " + e);
            return false;
        }
    }
}
