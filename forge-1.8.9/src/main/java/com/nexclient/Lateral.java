package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.GuiConnecting;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Las pantallas de Minecraft (opciones, multijugador, un jugador, logros, estadísticas y las que abren) dentro del
 * menú lateral de NexClient: la pantalla cree que es más angosta, se dibuja a la derecha del panel y el ratón se le
 * pasa corrido. El panel no se vuelve a animar al ir de una a otra.
 */
public final class Lateral {
    private static final Map<GuiScreen, PantallaLateral> RAIZ = new WeakHashMap<GuiScreen, PantallaLateral>();
    private static final Map<GuiScreen, Long> ABIERTA = new WeakHashMap<GuiScreen, Long>();
    static boolean dibujando;

    private Lateral() {
    }

    public static boolean es(Object s) {
        return s instanceof GuiScreen && RAIZ.containsKey(s);
    }

    public static PantallaLateral raiz(GuiScreen s) {
        return s == null ? null : RAIZ.get(s);
    }

    private static boolean envolvible(GuiScreen s) {
        if (s instanceof PantallaLateral && ((PantallaLateral) s).embebido) {
            return true;  // las páginas de NexClient dentro del panel
        }
        if (s == null || s.getClass().getName().startsWith("com.nexclient.")) {
            return false;  // las de NexClient tienen su propio panel o necesitan toda la pantalla
        }
        return !(s instanceof GuiContainer || s instanceof GuiChat || s instanceof GuiConnecting
                || s instanceof GuiDownloadTerrain || s instanceof GuiGameOver || s instanceof GuiMainMenu
                || s instanceof GuiIngameMenu || s instanceof GuiErrorScreen || s instanceof GuiDisconnected);
    }

    /** Al abrir una pantalla: si viene del menú principal o de pausa (o de una que ya va en el panel), va en el panel. */
    public static void alAbrir(GuiScreen anterior, GuiScreen nueva) {
        if (!Config.get().menuNex || !envolvible(nueva)) {
            return;
        }
        PantallaLateral r = anterior instanceof MenuPrincipalNex || anterior instanceof MenuPausaNex
                ? (PantallaLateral) anterior : RAIZ.get(anterior);
        if (r != null) {
            RAIZ.put(nueva, r);
            ABIERTA.put(nueva, System.nanoTime());
        }
    }

    /** Lo que mide de ancho la pantalla completa (en unidades de la interfaz). */
    static int anchoTotal() {
        return new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
    }

    /** La X del ratón (en píxeles de la ventana) que hace que la pantalla angosta calcule bien su posición. */
    public static int eventoX(GuiScreen s, int x) {
        Minecraft mc = Minecraft.getMinecraft();
        if (s.width <= 0 || mc.displayWidth <= 0) {
            return x;
        }
        double completo = x * (double) anchoTotal() / mc.displayWidth;
        return (int) Math.round((completo - MenuLateral.ANCHO) * mc.displayWidth / s.width);
    }

    /** Dónde empieza la pantalla: a la derecha del panel, entrando con un deslizamiento corto al abrirse. */
    static float desplazamiento(GuiScreen s) {
        Long t0 = ABIERTA.get(s);
        float t = t0 == null ? 1 : Math.min(1, (System.nanoTime() - t0) / 1e9f / 0.22f);
        float u = 1 - t;
        return MenuLateral.ANCHO + 14 * u * u * u;
    }
}
