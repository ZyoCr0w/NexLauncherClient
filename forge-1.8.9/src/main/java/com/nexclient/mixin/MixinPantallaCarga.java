package com.nexclient.mixin;

import com.nexclient.PantallaCarga;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Al iniciar, la pantalla de carga de NexClient en lugar del logo de Mojang. */
@Mixin(Minecraft.class)
public abstract class MixinPantallaCarga {
    @Inject(method = "drawSplashScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$carga(TextureManager tm, CallbackInfo ci) {
        PantallaCarga.dibujar();
        ci.cancel();
    }
}
