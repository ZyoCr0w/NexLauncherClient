package com.nexclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Oculta las llamas que tapan la pantalla cuando te quemas. */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nexclient$fuego(Minecraft mc, PoseStack pose, CallbackInfo ci) {
        if (Config.get().ocultarFuego) {
            ci.cancel();
        }
    }
}
