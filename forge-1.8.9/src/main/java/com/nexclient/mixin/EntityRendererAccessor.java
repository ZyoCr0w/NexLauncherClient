package com.nexclient.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Saturación: cargar un efecto de pantalla propio (el método del juego es privado). */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("loadShader")
    void nexclient$cargarEfecto(ResourceLocation ubicacion);
}
