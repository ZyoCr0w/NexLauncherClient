package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Method;

/**
 * Ranuras de la barra puestas en un botón del ratón (los laterales, por ejemplo).
 *
 * El juego solo se entera del clic por la cola de eventos del ratón; con algunos ratones y controladores esos clics se
 * pierden mientras mantienes otras teclas (caminar, agacharte). Aquí, además del evento, en cada cuadro se mira si el
 * botón está apretado (y en Windows se le pregunta directo al sistema, que lo ve aunque se pierda el mensaje):
 * en cuanto se aprieta, cambias de objeto.
 */
public final class RanuraRaton {
    private static final boolean[] antes = new boolean[16];
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    /** Teclas virtuales de Windows para los botones 0–4 (izquierdo, derecho, rueda, lateral 1, lateral 2). */
    private static final int[] VK = {0x01, 0x02, 0x04, 0x05, 0x06};
    private static Object getAsyncKeyState;
    private static Method invocarInt;
    private static boolean sinSistema;
    private static boolean avisoEvento, avisoSondeo;

    private RanuraRaton() {
    }

    /** Clic que llegó como evento del ratón. */
    public static void clic(Minecraft mc, int boton) {
        if (elegir(mc, boton) && !avisoEvento) {
            avisoEvento = true;
            System.out.println("[NexClient] Ranura por botón del ratón " + boton + " (evento)");
        }
    }

    /** Cada cuadro: el estado real del botón, por si el evento se perdió. */
    public static void sondear(Minecraft mc) {
        boolean jugando = mc.thePlayer != null && mc.currentScreen == null && mc.inGameHasFocus && Display.isActive();
        KeyBinding[] barra = mc.gameSettings.keyBindsHotbar;
        for (KeyBinding k : barra) {
            int b = k.getKeyCode() + 100;  // los botones del ratón se guardan como -100 + botón
            if (b < 0 || b >= antes.length) {
                continue;
            }
            boolean ahora = jugando && apretado(b);
            if (ahora && !antes[b] && elegir(mc, b) && !avisoSondeo) {
                avisoSondeo = true;
                System.out.println("[NexClient] Ranura por botón del ratón " + b + " (sondeo)");
            }
            antes[b] = ahora;
        }
    }

    private static boolean elegir(Minecraft mc, int boton) {
        if (mc.thePlayer == null || mc.currentScreen != null || mc.thePlayer.isSpectator()) {
            return false;
        }
        KeyBinding[] barra = mc.gameSettings.keyBindsHotbar;
        for (int i = 0; i < barra.length; i++) {
            if (barra[i].getKeyCode() == boton - 100) {
                mc.thePlayer.inventory.currentItem = i;
                return true;
            }
        }
        return false;
    }

    private static boolean apretado(int b) {
        try {
            if (b < Mouse.getButtonCount() && Mouse.isButtonDown(b)) {
                return true;
            }
        } catch (Throwable ignorado) {
            // sin ratón de LWJGL: se pregunta al sistema
        }
        return b < VK.length && sistema(VK[b]);
    }

    /** Windows: GetAsyncKeyState (con JNA, que viene con Minecraft) dice si el botón está abajo ahora mismo. */
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
