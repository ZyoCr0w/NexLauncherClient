package com.nexclient.mixin;

import com.nexclient.Capas;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Capa animada de NexLauncher (GIF o de Nex) en lugar de la del jugador; también se usa en la élitra. */
@Mixin(value = AbstractClientPlayer.class, priority = 1500)
public class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void nexclient$capa(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin s = cir.getReturnValue();
        if (s != null && Capas.capaModelo((AbstractClientPlayer) (Object) this)) {  // capa 3D: sin la normal
            cir.setReturnValue(new PlayerSkin(s.texture(), s.textureUrl(), null, null, s.model(), s.secure()));
            return;
        }
        ResourceLocation capa = Capas.capa((AbstractClientPlayer) (Object) this);
        if (capa != null && s != null) {
            cir.setReturnValue(new PlayerSkin(s.texture(), s.textureUrl(), capa, capa, s.model(), s.secure()));
        }
    }
}
