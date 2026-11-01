package com.nexclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class NexClient implements ClientModInitializer {
    private static void botonPausa(net.minecraft.client.Minecraft mc, net.minecraft.client.gui.screens.Screen pantalla) {
        java.util.List<net.minecraft.client.gui.components.AbstractWidget> widgets = Screens.getButtons(pantalla);
        net.minecraft.client.gui.components.AbstractWidget volver = null;
        for (net.minecraft.client.gui.components.AbstractWidget w : widgets) {
            if (w.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents
                    && ((net.minecraft.network.chat.contents.TranslatableContents) w.getMessage().getContents())
                    .getKey().equals("menu.returnToGame")) {
                volver = w;
            }
        }
        if (volver == null) {  // por si otro mod cambió el texto: el botón más alto
            for (net.minecraft.client.gui.components.AbstractWidget w : widgets) {
                if (w instanceof Button && (volver == null || w.getY() < volver.getY())) {
                    volver = w;
                }
            }
        }
        if (volver == null) {  // menú de pausa sin botones (F3 + Esc)
            return;
        }
        int y = volver.getY();
        for (net.minecraft.client.gui.components.AbstractWidget w : widgets) {
            if (w.getY() >= y) {
                w.setY(w.getY() + 24);  // todo baja una fila para hacerle lugar
            }
        }
        widgets.add(Button.builder(Component.literal("⚔ NexClient"), b -> mc.setScreen(new MenuScreen(pantalla)))
                .bounds(volver.getX(), y, volver.getWidth(), 20).build());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registrarCapa(net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper ayuda,
                                      net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
        ayuda.register((net.minecraft.client.renderer.entity.layers.RenderLayer) new CapaCosmeticos(renderer));
    }

    public static KeyMapping teclaMenu;
    private static int ticks;

    @Override
    public void onInitializeClient() {
        Config.cargar(FabricLoader.getInstance().getConfigDir().resolve("nexclient.json").toFile());
        teclaMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.nexclient.menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nexclient"));
        Movimiento.teclaZoom = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.nexclient.zoomz",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.nexclient"));
        VistaLibre.tecla = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.nexclient.vistalibre",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.nexclient"));
        ClientTickEvents.START_CLIENT_TICK.register(Movimiento::tick);
        ClientTickEvents.START_CLIENT_TICK.register(VistaLibre::tick);
        ClientTickEvents.END_CLIENT_TICK.register(Lan::tick);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (teclaMenu.consumeClick()) {
                mc.setScreen(new MenuScreen(mc.screen));
            }
            Efectos.tick(mc);
            if (mc.player != null && mc.level != null) {  // etiquetas y cosméticos de los jugadores cercanos
                java.util.List<String> cerca = new java.util.ArrayList<String>();
                for (net.minecraft.world.entity.player.Player otro : mc.level.players()) {
                    if (otro != mc.player && otro.distanceToSqr(mc.player) < 96 * 96) {
                        cerca.add(otro.getGameProfile().getName());
                    }
                }
                Cosmeticos.tick(mc.player.getGameProfile().getName(),
                        mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "", cerca);
            }
            if (++ticks % 40 == 0 && mc.screen == null) {
                Config.recargarSiCambio();
            }
        });
        HudRenderCallback.EVENT.register((g, tiempo) -> Hud.dibujar(g, false));
        // cosméticos 3D de NexLauncher sobre los jugadores
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (tipo, renderer, ayuda, ctx) -> {
                    if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer) {
                        registrarCapa(ayuda, (net.minecraft.client.renderer.entity.player.PlayerRenderer) renderer);
                    }
                });
        ClientTickEvents.END_CLIENT_TICK.register(Rastros::tick);
        // Botón «NexClient» en el menú de pausa (Esc): centrado, justo encima de «Volver al juego»
        ScreenEvents.AFTER_INIT.register((mc, pantalla, ancho, alto) -> {
            if (pantalla instanceof PauseScreen) {
                botonPausa(mc, pantalla);
            }
        });
        AttackEntityCallback.EVENT.register((jugador, mundo, mano, objetivo, golpe) -> {
            if (mundo.isClientSide()) {
                Efectos.golpe(jugador, objetivo);
            }
            return InteractionResult.PASS;
        });
    }
}
