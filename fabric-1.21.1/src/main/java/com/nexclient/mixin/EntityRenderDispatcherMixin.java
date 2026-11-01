package com.nexclient.mixin;

import com.nexclient.Etiquetas;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** El inventario apaga las sombras mientras dibuja a tu personaje: así se sabe que no es el mundo. */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "setRenderShadow", at = @At("HEAD"), require = 0)
    private void nexclient$sombras(boolean v, CallbackInfo ci) {
        Etiquetas.enMundo = v;
    }
}
