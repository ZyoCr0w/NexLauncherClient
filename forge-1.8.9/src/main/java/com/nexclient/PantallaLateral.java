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
    /** Dentro del panel de otro menú (las páginas de NexClient): sin panel propio, solo el contenido. */
    public boolean embebido;

    protected PantallaLateral(boolean principal) {
        menu = new MenuLateral(principal);
    }

    /** Llena menu.entradas (se llama en cada initGui, p. ej. al cambiar el tamaño de la ventana). */
    protected abstract void entradas();

    protected MenuLateral.Entrada entrada(String texto, Runnable accion, boolean separador) {
        MenuLateral.Entrada e = new MenuLateral.Entrada(texto, accion, separador, false, true);
        menu.entradas.add(e);
        return e;
    }

    /** Entrada con submenú (se despliega en el mismo panel). */
    protected MenuLateral.Entrada grupo(String texto, boolean separador) {
        return entrada(texto, null, separador);
    }

    protected static void hijo(MenuLateral.Entrada grupo, String texto, Runnable accion) {
        grupo.hijos.add(new MenuLateral.Entrada(texto, accion, false, false, true));
    }

    /** NexClient ▸ sus páginas, que se ven a la derecha del panel. */
    protected void grupoNexClient(boolean separador) {
        final GuiScreen yo = this;
        MenuLateral.Entrada g = grupo("NexClient", separador);
        for (int i = 0; i < MenuNex.PAGINAS.length; i++) {
            final int k = i;
            hijo(g, MenuNex.PAGINAS[i], new Runnable() {
                public void run() { mc.displayGuiScreen(new MenuNex(yo, k)); } });
        }
    }

    private static String sinPuntos(String s) {
        return s.replace("...", "").replace("…", "").trim();
    }

    /** Opciones ▸ General, Video, Sonido, Controles, Idioma, Chat, Paquetes y Skin. */
    protected void grupoOpciones(boolean separador) {
        final GuiScreen yo = this;
        MenuLateral.Entrada g = grupo(sinPuntos(net.minecraft.client.resources.I18n.format("menu.options")), separador);
        hijo(g, "General", new Runnable() {
            public void run() { mc.displayGuiScreen(new net.minecraft.client.gui.GuiOptions(yo, mc.gameSettings)); } });
        hijo(g, sinPuntos(net.minecraft.client.resources.I18n.format("options.video")), new Runnable() {
            public void run() { mc.displayGuiScreen(new net.minecraft.client.gui.GuiVideoSettings(yo, mc.gameSettings)); } });
        hijo(g, sinPuntos(net.minecraft.client.resources.I18n.format("options.sounds")), new Runnable() {
            public void run() { mc.displayGuiScreen(new net.minecraft.client.gui.GuiScreenOptionsSounds(yo, mc.gameSettings)); } });
        hijo(g, sinPuntos(net.minecraft.client.resources.I18n.format("options.controls")), new Runnable() {
            public void run() { mc.displayGuiScreen(new net.minecraft.client.gui.GuiControls(yo, mc.gameSettings)); } });
        hijo(g, sinPuntos(net.minecraft.client.resources.I18n.format("options.language")), new Runnable() {
            public void run() {
                mc.displayGuiScreen(new net.minecraft.client.gui.GuiLanguage(yo, mc.gameSettings, mc.getLanguageManager()));
            } });
        hijo(g, sinPuntos(net.minecraft.client.resources.I18n.format("options.chat.title")), new Runnable() {
            public void run() { mc.displayGuiScreen(new net.minecraft.client.gui.ScreenChatOptions(yo, mc.gameSettings)); } });
        hijo(g, sinPuntos(net.minecraft.client.resources.I18n.format("options.resourcepack")), new Runnable() {
            public void run() { mc.displayGuiScreen(new net.minecraft.client.gui.GuiScreenResourcePacks(yo)); } });
        hijo(g, sinPuntos(net.minecraft.client.resources.I18n.format("options.skinCustomisation")), new Runnable() {
            public void run() { mc.displayGuiScreen(new net.minecraft.client.gui.GuiCustomizeSkin(yo)); } });
    }

    @Override
    public void initGui() {
        buttonList.clear();
        java.util.Set<String> abiertos = new java.util.HashSet<String>();  // los submenús abiertos siguen abiertos
        for (MenuLateral.Entrada e : menu.entradas) {
            if (e.abierto) {
                abiertos.add(e.texto);
            }
        }
        menu.entradas.clear();
        if (!embebido) {
            entradas();
        }
        for (MenuLateral.Entrada e : menu.entradas) {
            e.abierto = abiertos.contains(e.texto);
        }
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
        if (!embebido) {
            menu.dibujar(this, width, height, mx, my);
        }
        super.drawScreen(mx, my, parcial);  // botones propios de la pantalla (el menú de NexClient)
    }

    @Override
    protected void mouseClicked(int mx, int my, int boton) throws IOException {
        if (boton != 0 || !clicLateral(mx, my)) {
            super.mouseClicked(mx, my, boton);
        }
    }

    /** Clic en el panel (también cuando hay otra pantalla de Minecraft a su derecha). */
    public boolean clicLateral(int mx, int my) {
        MenuLateral.Entrada e = embebido ? null : menu.en(mx, my);
        if (e == null) {
            return false;
        }
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        if (!e.hijos.isEmpty()) {
            menu.alternar(e);
            return true;
        }
        menu.marcada = e;
        if (e.accion != null) {
            e.accion.run();
        }
        return true;
    }

    /** Dibuja solo el panel (y su fondo), para las pantallas que van a su derecha. */
    public void dibujarMenu(int w, int h, int mx, int my) {
        menu.dibujar(this, w, h, mx, my);
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
