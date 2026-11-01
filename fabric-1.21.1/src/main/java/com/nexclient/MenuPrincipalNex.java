package com.nexclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.language.I18n;

/** Menú principal de NexClient (reemplaza al de Minecraft y al de cualquier paquete de texturas). */
public class MenuPrincipalNex extends PantallaLateral {
    public MenuPrincipalNex() {
        super(true);
    }

    @Override
    protected void entradas() {
        entrada(I18n.get("menu.singleplayer"), () -> minecraft.setScreen(new SelectWorldScreen(this)), false);
        entrada(I18n.get("menu.multiplayer"), () -> minecraft.setScreen(new JoinMultiplayerScreen(this)), false);
        grupoNexClient(true);
        grupoOpciones(false);
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            entrada("Mods", () -> {
                try {
                    minecraft.setScreen((Screen) Class.forName("com.terraformersmc.modmenu.gui.ModsScreen")
                            .getConstructor(Screen.class).newInstance(this));
                } catch (ReflectiveOperationException | RuntimeException ignorada) {
                    // otra versión de Mod Menu: sin el botón
                }
            }, false);
        }
        menu.entradas.add(new MenuLateral.Entrada(I18n.get("menu.quit"), () -> minecraft.stop(), true, true, true));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
