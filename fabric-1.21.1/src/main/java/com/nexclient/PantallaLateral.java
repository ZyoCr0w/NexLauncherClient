package com.nexclient;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/** Base del menú principal y de pausa de NexClient en 1.21.1 (el dibujo está en MenuLateral). */
public abstract class PantallaLateral extends Screen {
    private static final ResourceLocation LOGO = ResourceLocation.fromNamespaceAndPath("nexclient", "textures/gui/logo.png");
    private static final ResourceLocation BRILLO = ResourceLocation.fromNamespaceAndPath("nexclient", "textures/gui/brillo.png");
    protected final MenuLateral menu;
    private GuiGraphics g;

    protected PantallaLateral(boolean principal) {
        super(Component.literal("NexClient"));
        menu = new MenuLateral(principal);
    }

    /** Llena menu.entradas (se llama en cada init, p. ej. al cambiar el tamaño de la ventana). */
    protected abstract void entradas();

    protected void entrada(String texto, Runnable accion, boolean separador) {
        menu.entradas.add(new MenuLateral.Entrada(texto, accion, separador, false, true));
    }

    @Override
    protected void init() {
        menu.entradas.clear();
        entradas();
        menu.subtitulo = "Minecraft " + SharedConstants.getCurrentVersion().getName();
        menu.pie1 = minecraft.getUser().getName();
        menu.pie2 = "NexClient " + FabricLoader.getInstance().getModContainer("nexclient")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float parcial) {
        // el fondo lo pone MenuLateral (sin el desenfoque ni el panorama de Minecraft)
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float parcial) {
        this.g = g;
        RenderSystem.enableBlend();
        menu.dibujar(pincel, width, height, mx, my);
        this.g = null;
        super.render(g, mx, my, parcial);  // botones propios de la pantalla (el menú de NexClient)
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        MenuLateral.Entrada e = boton == 0 ? menu.en((int) mx, (int) my) : null;
        if (e != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            e.accion.run();
            return true;
        }
        return super.mouseClicked(mx, my, boton);
    }

    private final MenuLateral.Pincel pincel = new MenuLateral.Pincel() {
        @Override
        public void rect(int x1, int y1, int x2, int y2, int argb) {
            if ((argb >>> 24) != 0) {
                g.fill(x1, y1, x2, y2, argb);
            }
        }

        @Override
        public void degradado(int x1, int y1, int x2, int y2, int arriba, int abajo) {
            g.fillGradient(x1, y1, x2, y2, arriba, abajo);
        }

        @Override
        public void texto(String s, float x, float y, int argb, float escala, boolean sombra) {
            if ((argb >>> 24) < 8 || s == null || s.isEmpty()) {
                return;  // con menos alfa el juego lo dibujaría opaco
            }
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(escala, escala, 1);
            g.drawString(font, s, 0, 0, argb, sombra);
            g.pose().popPose();
        }

        @Override
        public int ancho(String s) {
            return font.width(s);
        }

        @Override
        public void logo(int x, int y, int tam, float alfa) {
            textura(LOGO, 256, x, y, tam, tam, alfa);
        }

        @Override
        public void brillo(int x, int y, int w, int h, float alfa) {
            textura(BRILLO, 128, x, y, w, h, alfa);
        }

        private void textura(ResourceLocation r, int lado, int x, int y, int w, int h, float alfa) {
            if (alfa <= 0.01f) {
                return;
            }
            RenderSystem.enableBlend();
            g.setColor(1, 1, 1, alfa);
            g.blit(r, x, y, w, h, 0, 0, lado, lado, lado, lado);
            g.setColor(1, 1, 1, 1);
        }
    };
}
