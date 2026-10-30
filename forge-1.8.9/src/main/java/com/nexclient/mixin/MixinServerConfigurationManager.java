package com.nexclient.mixin;

import com.nexclient.SkinsLan;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ServerConfigurationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Antes de avisar a todos que entró alguien a tu mundo, se le pone su skin de NexLauncher. */
@Mixin(ServerConfigurationManager.class)
public class MixinServerConfigurationManager {
    @Inject(method = "playerLoggedIn", at = @At("HEAD"), require = 0)
    private void nexclient$skin(EntityPlayerMP jugador, CallbackInfo ci) {
        SkinsLan.completar(jugador.getGameProfile());
    }
}
