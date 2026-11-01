package com.nexclient.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Saturación: cargar un efecto de pantalla propio (el método del juego es privado). */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("loadEffect")
    void nexclient$cargarEfecto(ResourceLocation ubicacion);
}
