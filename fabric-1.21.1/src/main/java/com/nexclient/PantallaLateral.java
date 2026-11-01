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
    /** Dentro del panel de otro menú (las páginas de NexClient): sin panel propio, solo el contenido. */
    public boolean embebido;
    private GuiGraphics g;

    protected PantallaLateral(boolean principal) {
        super(Component.literal("NexClient"));
        menu = new MenuLateral(principal);
    }

    /** Llena menu.entradas (se llama en cada init, p. ej. al cambiar el tamaño de la ventana). */
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

    private static String sinPuntos(String s) {
        return s.replace("...", "").replace("…", "").trim();
    }

    /** NexClient ▸ sus páginas, que se ven a la derecha del panel. */
    protected void grupoNexClient(boolean separador) {
        MenuLateral.Entrada g = grupo("NexClient", separador);
        for (int i = 0; i < MenuScreen.PAGINAS.length; i++) {
            final int k = i;
            hijo(g, MenuScreen.PAGINAS[i], () -> minecraft.setScreen(new MenuScreen(this, k)));
        }
    }

    /** Opciones ▸ General, Video, Sonido, Controles, Idioma, Chat, Paquetes, Skin y Accesibilidad. */
    protected void grupoOpciones(boolean separador) {
        net.minecraft.client.Options o = minecraft.options;
        MenuLateral.Entrada g = grupo(sinPuntos(net.minecraft.client.resources.language.I18n.get("menu.options")), separador);
        hijo(g, "General", () -> minecraft.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this, o)));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.video")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.options.VideoSettingsScreen(this, minecraft, o)));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.sounds")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.options.SoundOptionsScreen(this, o)));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.controls")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.options.controls.ControlsScreen(this, o)));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.language")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.options.LanguageSelectScreen(this, o, minecraft.getLanguageManager())));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.chat.title")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.options.ChatOptionsScreen(this, o)));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.resourcepack")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.packs.PackSelectionScreen(minecraft.getResourcePackRepository(), repo -> {
                    o.updateResourcePacks(repo);
                    minecraft.setScreen(this);
                }, minecraft.getResourcePackDirectory(), Component.translatable("resourcePack.title"))));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.skinCustomisation")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.options.SkinCustomizationScreen(this, o)));
        hijo(g, sinPuntos(net.minecraft.client.resources.language.I18n.get("options.accessibility")), () -> minecraft.setScreen(
                new net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen(this, o)));
    }

    @Override
    protected void init() {
        java.util.Set<String> abiertos = new java.util.HashSet<>();  // los submenús abiertos siguen abiertos
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
        if (!embebido) {
            menu.dibujar(pincel, width, height, mx, my);
        }
        this.g = null;
        super.render(g, mx, my, parcial);  // botones propios de la pantalla (el menú de NexClient)
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        if (boton == 0 && clicLateral(mx, my)) {
            return true;
        }
        return super.mouseClicked(mx, my, boton);
    }

    /** Clic en el panel (también cuando hay otra pantalla de Minecraft a su derecha). */
    public boolean clicLateral(double mx, double my) {
        MenuLateral.Entrada e = embebido ? null : menu.en((int) mx, (int) my);
        if (e == null) {
            return false;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
    public void dibujarMenu(GuiGraphics g, int w, int h, int mx, int my) {
        this.g = g;
        RenderSystem.enableBlend();
        menu.dibujar(pincel, w, h, mx, my);
        this.g = null;
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
