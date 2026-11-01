package com.nexclient;

import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** Menú de pausa (Esc) de NexClient: el juego se ve detrás, oscurecido. */
public class MenuPausaNex extends PantallaLateral {
    private boolean saliendo;

    public MenuPausaNex() {
        super(false);
    }

    @Override
    protected void entradas() {
        entrada(I18n.get("menu.returnToGame"), () -> minecraft.setScreen(null), false);
        entrada("NexClient", () -> minecraft.setScreen(new MenuScreen(this)), false);
        entrada(I18n.get("menu.options").replace("...", ""),
                () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)), true);
        entrada(I18n.get("gui.advancements"), () -> {
            if (minecraft.player != null) {
                minecraft.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this));
            }
        }, false);
        entrada(I18n.get("gui.stats"), () -> {
            if (minecraft.player != null) {
                minecraft.setScreen(new StatsScreen(this, minecraft.player.getStats()));
            }
        }, false);
        boolean lan = minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null
                && !minecraft.getSingleplayerServer().isPublished();
        menu.entradas.add(new MenuLateral.Entrada(I18n.get("menu.shareToLan"),
                () -> minecraft.setScreen(new ShareToLanScreen(this)), false, false, lan));
        String salir = I18n.get(minecraft.isLocalServer() ? "menu.returnToMenu" : "menu.disconnect");
        menu.entradas.add(new MenuLateral.Entrada(salir, this::salir, true, true, true));
    }

    @Override
    protected void init() {
        super.init();
        menu.subtitulo = I18n.get("menu.game").replace(":", "");
    }

    /** Lo mismo que el botón de Minecraft. */
    private void salir() {
        if (saliendo) {
            return;
        }
        saliendo = true;
        boolean local = minecraft.isLocalServer();
        ServerData servidor = minecraft.getCurrentServer();
        if (minecraft.level != null) {
            minecraft.level.disconnect();
        }
        if (local) {
            minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            minecraft.disconnect();
        }
        TitleScreen titulo = new TitleScreen();
        if (local) {
            minecraft.setScreen(titulo);
        } else if (servidor != null && servidor.isRealm()) {
            minecraft.setScreen(new com.mojang.realmsclient.RealmsMainScreen(titulo));
        } else {
            minecraft.setScreen(new JoinMultiplayerScreen(titulo));
        }
    }
}
