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
        Tr.idioma = () -> net.minecraft.client.Minecraft.getMinecraft().gameSettings.language;
        Config.cargar(new File(e.getModConfigurationDirectory(), "nexclient.json"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        ClientRegistry.registerKeyBinding(TECLA_MENU);
        Movimiento.teclaZoom = new KeyBinding("Zoom NexClient", Keyboard.KEY_Z, "NexClient");
        ClientRegistry.registerKeyBinding(Movimiento.teclaZoom);
        VistaLibre.tecla = new KeyBinding("Vista libre (NexClient)", Keyboard.KEY_R, "NexClient");
        ClientRegistry.registerKeyBinding(VistaLibre.tecla);
        MinecraftForge.EVENT_BUS.register(new Eventos());
    }

    /** Cosméticos 3D de NexLauncher en los dos modelos de jugador (brazos normales y finos). */
    @Mod.EventHandler
    public void postInit(net.minecraftforge.fml.common.event.FMLPostInitializationEvent e) {
        for (net.minecraft.client.renderer.entity.RenderPlayer r
                : net.minecraft.client.Minecraft.getMinecraft().getRenderManager().getSkinMap().values()) {
            r.addLayer(new CapaCosmeticos(r));
        }
    }
}
