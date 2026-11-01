package com.nexclient.mixin;

import com.nexclient.Capas;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Capa animada de NexLauncher (GIF o de Nex) en lugar de la del jugador (también antes que la de OptiFine). */
@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {
    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$capa(CallbackInfoReturnable<ResourceLocation> cir) {
        if (Capas.capaModelo((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(null);  // lleva una capa 3D: sin la capa normal
            return;
        }
        ResourceLocation capa = Capas.capa((AbstractClientPlayer) (Object) this);
        if (capa != null) {
            cir.setReturnValue(capa);
        }
    }
}
