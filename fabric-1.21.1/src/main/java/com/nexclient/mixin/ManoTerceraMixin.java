package com.nexclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexclient.PortadorEscudo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Anota quién sostiene el objeto en tercera persona (para el escudo de NexLauncher). */
@Mixin(ItemInHandLayer.class)
public class ManoTerceraMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), require = 0)
    private void nexclient$antes(LivingEntity e, ItemStack s, ItemDisplayContext c, HumanoidArm b, PoseStack p,
                                 MultiBufferSource m, int l, CallbackInfo ci) {
        PortadorEscudo.poner(e instanceof AbstractClientPlayer ? (AbstractClientPlayer) e : null);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"), require = 0)
    private void nexclient$despues(LivingEntity e, ItemStack s, ItemDisplayContext c, HumanoidArm b, PoseStack p,
                                   MultiBufferSource m, int l, CallbackInfo ci) {
        PortadorEscudo.poner(null);
    }
}
