package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Una pantalla de Minecraft (opciones, multijugador, un jugador, progresos, estadísticas y las que abren) dentro del
 * menú lateral de NexClient: el panel a la izquierda y la pantalla, más angosta, a su derecha. Todo lo que hace el
 * jugador se le pasa con el ratón corrido.
 */
public class PantallaEnvuelta extends Screen {
    /** Las pantallas que van dentro del panel (no dibujan su propio fondo: se ve el de NexClient). */
    public static final Set<Screen> HIJAS = Collections.newSetFromMap(new WeakHashMap<Screen, Boolean>());
    public final PantallaLateral raiz;
    public final Screen hija;
    private final long t0 = System.nanoTime();

    public PantallaEnvuelta(PantallaLateral raiz, Screen hija) {
        super(hija.getTitle());
        this.raiz = raiz;
        this.hija = hija;
        HIJAS.add(hija);
    }

    private static final int A = MenuLateral.ANCHO;

    /** ¿Va dentro del panel? Las de NexClient tienen su propio panel o necesitan toda la pantalla. */
    public static boolean envolvible(Screen s) {
        if (s instanceof PantallaLateral && ((PantallaLateral) s).embebido) {
            return true;  // las páginas de NexClient dentro del panel
        }
        if (s == null || s.getClass().getName().startsWith("com.nexclient.")) {
            return false;
        }
        return !(s instanceof AbstractContainerScreen || s instanceof ChatScreen || s instanceof ConnectScreen
                || s instanceof ReceivingLevelScreen || s instanceof LevelLoadingScreen || s instanceof GenericMessageScreen
                || s instanceof ProgressScreen || s instanceof DeathScreen || s instanceof TitleScreen
                || s instanceof PauseScreen);
    }

    /** Con qué menú de NexClient va una pantalla nueva que se abre desde «actual» (o null si va sola). */
    public static PantallaLateral raizDe(Screen actual) {
        if (actual instanceof MenuPrincipalNex || actual instanceof MenuPausaNex) {
            return (PantallaLateral) actual;
        }
        return actual instanceof PantallaEnvuelta ? ((PantallaEnvuelta) actual).raiz : null;
    }

    @Override
    protected void init() {
        hija.init(minecraft, Math.max(50, width - A), height);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
        // el fondo lo pone el panel
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float parcial) {
        raiz.dibujarMenu(g, width, height, mx, my);
        float t = Math.min(1, (System.nanoTime() - t0) / 1e9f / 0.22f), u = 1 - t;
        g.pose().pushPose();
        g.pose().translate(A + 14 * u * u * u, 0, 0);
        hija.renderWithTooltip(g, mx - A, my, parcial);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double x, double y, int boton) {
        if (x < A) {
            return boton == 0 && raiz.clicLateral(x, y);
        }
        return hija.mouseClicked(x - A, y, boton);
    }

    @Override
    public boolean mouseReleased(double x, double y, int boton) {
        return hija.mouseReleased(x - A, y, boton);
    }

    @Override
    public boolean mouseDragged(double x, double y, int boton, double dx, double dy) {
        return hija.mouseDragged(x - A, y, boton, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        return hija.mouseScrolled(x - A, y, h, v);
    }

    @Override
    public void mouseMoved(double x, double y) {
        hija.mouseMoved(x - A, y);
    }

    @Override
    public boolean keyPressed(int tecla, int codigo, int mods) {
        return hija.keyPressed(tecla, codigo, mods);
    }

    @Override
    public boolean keyReleased(int tecla, int codigo, int mods) {
        return hija.keyReleased(tecla, codigo, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        return hija.charTyped(c, mods);
    }

    @Override
    public void tick() {
        hija.tick();
    }

    @Override
    public void removed() {
        hija.removed();
    }

    @Override
    public void onClose() {
        hija.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return hija.isPauseScreen();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return hija.shouldCloseOnEsc();
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        this.width = w;
        this.height = h;
        hija.resize(mc, Math.max(50, w - A), h);
    }
}
