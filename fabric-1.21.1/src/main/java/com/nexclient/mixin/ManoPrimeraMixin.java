package com.nexclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexclient.PortadorEscudo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Anota quién sostiene el objeto en primera persona (para el escudo de NexLauncher). */
@Mixin(value = ItemInHandRenderer.class, priority = 900)
public class ManoPrimeraMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void nexclient$antes(AbstractClientPlayer p, float t, float pitch, InteractionHand mano, float golpe,
                                 ItemStack s, float equipado, PoseStack pose, MultiBufferSource m, int luz, CallbackInfo ci) {
        PortadorEscudo.poner(p);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void nexclient$despues(AbstractClientPlayer p, float t, float pitch, InteractionHand mano, float golpe,
                                   ItemStack s, float equipado, PoseStack pose, MultiBufferSource m, int luz, CallbackInfo ci) {
        PortadorEscudo.poner(null);
    }
}
