package com.nexclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class NexClient implements ClientModInitializer {
    public static KeyMapping teclaMenu;
    private static int ticks;

    @Override
    public void onInitializeClient() {
        Config.cargar(FabricLoader.getInstance().getConfigDir().resolve("nexclient.json").toFile());
        teclaMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.nexclient.menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nexclient"));
        Movimiento.teclaZoom = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.nexclient.zoom",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.nexclient"));
        ClientTickEvents.START_CLIENT_TICK.register(Movimiento::tick);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (teclaMenu.consumeClick()) {
                mc.setScreen(new MenuScreen(mc.screen));
            }
            Efectos.tick(mc);
            if (++ticks % 40 == 0 && mc.screen == null) {
                Config.recargarSiCambio();
            }
        });
        HudRenderCallback.EVENT.register((g, tiempo) -> Hud.dibujar(g, false));
        AttackEntityCallback.EVENT.register((jugador, mundo, mano, objetivo, golpe) -> {
            if (mundo.isClientSide()) {
                Efectos.golpe(jugador, objetivo);
            }
            return InteractionResult.PASS;
        });
    }
}
