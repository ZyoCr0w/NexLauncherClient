package com.nexclient;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiShareToLan;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;

/** Menú de pausa (Esc) de NexClient: el juego se ve detrás, oscurecido. */
public class MenuPausaNex extends PantallaLateral {
    private boolean saliendo;

    public MenuPausaNex() {
        super(false);
    }

    @Override
    protected void entradas() {
        final MenuPausaNex yo = this;
        entrada(I18n.format("menu.returnToGame"), new Runnable() {
            public void run() {
                mc.displayGuiScreen(null);
                mc.setIngameFocus();
            } }, false);
        grupoNexClient(true);
        grupoOpciones(false);
        entrada(I18n.format("gui.achievements"), new Runnable() {
            public void run() {
                if (mc.thePlayer != null) {
                    mc.displayGuiScreen(new GuiAchievements(yo, mc.thePlayer.getStatFileWriter()));
                }
            } }, false);
        entrada(I18n.format("gui.stats"), new Runnable() {
            public void run() {
                if (mc.thePlayer != null) {
                    mc.displayGuiScreen(new GuiStats(yo, mc.thePlayer.getStatFileWriter()));
                }
            } }, false);
        boolean lan = mc.isSingleplayer() && mc.getIntegratedServer() != null && !mc.getIntegratedServer().getPublic();
        menu.entradas.add(new MenuLateral.Entrada(I18n.format("menu.shareToLan"), new Runnable() {
            public void run() { mc.displayGuiScreen(new GuiShareToLan(yo)); } }, false, false, lan));
        String salir = I18n.format(mc.isIntegratedServerRunning() ? "menu.returnToMenu" : "menu.disconnect");
        menu.entradas.add(new MenuLateral.Entrada(salir, new Runnable() {
            public void run() { salir(); } }, true, true, true));
    }

    /** Lo mismo que el botón de Minecraft. */
    private void salir() {
        if (saliendo) {
            return;
        }
        saliendo = true;
        boolean local = mc.isIntegratedServerRunning();
        WorldClient mundo = mc.theWorld;
        if (mundo != null) {
            mundo.sendQuittingDisconnectingPacket();
        }
        mc.loadWorld(null);
        if (local) {
            mc.displayGuiScreen(new GuiMainMenu());
        } else {
            mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        menu.subtitulo = I18n.format("menu.game").replace(":", "");
    }
}
