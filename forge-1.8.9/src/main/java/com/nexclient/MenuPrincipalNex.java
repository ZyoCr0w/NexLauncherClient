package com.nexclient;

import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.GuiModList;

/** Menú principal de NexClient (reemplaza al de Minecraft y al de cualquier paquete de texturas). */
public class MenuPrincipalNex extends PantallaLateral {
    public MenuPrincipalNex() {
        super(true);
    }

    @Override
    protected void entradas() {
        final MenuPrincipalNex yo = this;
        entrada(I18n.format("menu.singleplayer"), new Runnable() {
            public void run() { mc.displayGuiScreen(new GuiSelectWorld(yo)); } }, false);
        entrada(I18n.format("menu.multiplayer"), new Runnable() {
            public void run() { mc.displayGuiScreen(new GuiMultiplayer(yo)); } }, false);
        entrada("NexClient", new Runnable() {
            public void run() { mc.displayGuiScreen(new MenuNex(yo)); } }, false);
        entrada(I18n.format("menu.options").replace("...", ""), new Runnable() {
            public void run() { mc.displayGuiScreen(new GuiOptions(yo, mc.gameSettings)); } }, true);
        entrada(I18n.format("fml.menu.mods"), new Runnable() {
            public void run() { mc.displayGuiScreen(new GuiModList(yo)); } }, false);
        entrada(I18n.format("options.language").replace("...", ""), new Runnable() {
            public void run() { mc.displayGuiScreen(new GuiLanguage(yo, mc.gameSettings, mc.getLanguageManager())); } },
                false);
        menu.entradas.add(new MenuLateral.Entrada(I18n.format("menu.quit"), new Runnable() {
            public void run() { mc.shutdown(); } }, true, true, true));
    }

    @Override
    protected void keyTyped(char c, int tecla) {
        // Esc no hace nada en el menú principal
    }
}
