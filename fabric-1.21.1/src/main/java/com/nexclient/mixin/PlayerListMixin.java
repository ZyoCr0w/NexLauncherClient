package com.nexclient.mixin;

import com.nexclient.SkinsLan;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Antes de avisar a todos que entró alguien a tu mundo, se le pone su skin de NexLauncher. */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("HEAD"), require = 0)
    private void nexclient$skin(Connection conexion, ServerPlayer jugador, CommonListenerCookie cookie, CallbackInfo ci) {
        SkinsLan.completar(jugador.getGameProfile());
    }
}
