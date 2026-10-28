package com.nexclient;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

import java.io.File;

@Mod(modid = "nexclient", useMetadata = true)
public class NexClient {
    public static final KeyBinding TECLA_MENU = new KeyBinding("Menú de NexClient", Keyboard.KEY_RSHIFT, "NexClient");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Config.cargar(new File(e.getModConfigurationDirectory(), "nexclient.json"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        ClientRegistry.registerKeyBinding(TECLA_MENU);
        MinecraftForge.EVENT_BUS.register(new Eventos());
    }
}
