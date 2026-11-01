package com.nexclient.mixin;

import com.nexclient.Etiquetas;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** En F5 el juego no dibuja tu nombre; con la opción activada, sí. */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$nombrePropio(LivingEntity e, CallbackInfoReturnable<Boolean> cir) {
        if (Etiquetas.nombrePropio(e)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
