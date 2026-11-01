package com.nexclient;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.IOException;

/** Base del menú principal y de pausa de NexClient en 1.8.9 (el dibujo está en MenuLateral). */
public abstract class PantallaLateral extends GuiScreen implements MenuLateral.Pincel {
    private static final ResourceLocation LOGO = new ResourceLocation("nexclient", "textures/gui/logo.png");
    private static final ResourceLocation BRILLO = new ResourceLocation("nexclient", "textures/gui/brillo.png");
    protected final MenuLateral menu;

    protected PantallaLateral(boolean principal) {
        menu = new MenuLateral(principal);
    }

    /** Llena menu.entradas (se llama en cada initGui, p. ej. al cambiar el tamaño de la ventana). */
    protected abstract void entradas();

    protected void entrada(String texto, Runnable accion, boolean separador) {
        menu.entradas.add(new MenuLateral.Entrada(texto, accion, separador, false, true));
    }

    @Override
    public void initGui() {
        buttonList.clear();
        menu.entradas.clear();
        entradas();
        String v = version();
        menu.subtitulo = "Minecraft 1.8.9";
        menu.pie1 = mc.getSession().getUsername();
        menu.pie2 = "NexClient " + v;
    }

    static String version() {
        ModContainer m = Loader.instance().getIndexedModList().get("nexclient");
        return m == null ? "" : m.getVersion();
    }

    @Override
    public void drawScreen(int mx, int my, float parcial) {
        menu.dibujar(this, width, height, mx, my);
    }

    @Override
    protected void mouseClicked(int mx, int my, int boton) throws IOException {
        MenuLateral.Entrada e = boton == 0 ? menu.en(mx, my) : null;
        if (e != null) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
            e.accion.run();
        }
    }

    // ---- pincel
    @Override
    public void rect(int x1, int y1, int x2, int y2, int argb) {
        if ((argb >>> 24) != 0) {
            Gui.drawRect(x1, y1, x2, y2, argb);
        }
    }

    @Override
    public void degradado(int x1, int y1, int x2, int y2, int arriba, int abajo) {
        drawGradientRect(x1, y1, x2, y2, arriba, abajo);
    }

    @Override
    public void texto(String s, float x, float y, int argb, float escala, boolean sombra) {
        if ((argb >>> 24) < 8 || s == null || s.isEmpty()) {
            return;  // con menos alfa el juego lo dibujaría opaco
        }
        GlStateManager.enableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(escala, escala, 1);
        fontRendererObj.drawString(s, 0, 0, argb, sombra);
        GlStateManager.popMatrix();
    }

    @Override
    public int ancho(String s) {
        return fontRendererObj.getStringWidth(s);
    }

    @Override
    public void logo(int x, int y, int tam, float alfa) {
        textura(LOGO, x, y, tam, tam, alfa);
    }

    @Override
    public void brillo(int x, int y, int w, int h, float alfa) {
        textura(BRILLO, x, y, w, h, alfa);
    }

    private void textura(ResourceLocation r, int x, int y, int w, int h, float alfa) {
        if (alfa <= 0.01f) {
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, alfa);
        mc.getTextureManager().bindTexture(r);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, w, h, w, h);
        GlStateManager.color(1, 1, 1, 1);
    }
}
