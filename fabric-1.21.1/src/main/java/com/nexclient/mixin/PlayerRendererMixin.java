package com.nexclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexclient.Cosmeticos;
import com.nexclient.Etiquetas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nombre más arriba si el jugador lleva algo en la cabeza, y la etiqueta de NexLauncher encima del nombre. */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"), require = 0)
    private void nexclient$subir(AbstractClientPlayer p, Component nombre, PoseStack pose, MultiBufferSource buffers,
                                 int luz, float parcial, CallbackInfo ci) {
        pose.pushPose();
        pose.translate(0.0, Cosmeticos.subidaNombre(p.getGameProfile().getName(), p == Minecraft.getInstance().player), 0.0);
    }

    @Inject(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("TAIL"), require = 0)
    private void nexclient$etiqueta(AbstractClientPlayer p, Component nombre, PoseStack pose, MultiBufferSource buffers,
                                    int luz, float parcial, CallbackInfo ci) {
        try {
            Etiquetas.dibujar(p, pose, buffers, luz, parcial);
        } finally {
            pose.popPose();
        }
    }
}
