package com.nexclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexclient.CapaCosmeticos;
import com.nexclient.Cosmeticos;
import com.nexclient.Cosmeticos3D;
import com.nexclient.PortadorEscudo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Escudo de NexLauncher: reemplaza el modelo del escudo del jugador que lo tiene equipado (en 1.21). */
@Mixin(BlockEntityWithoutLevelRenderer.class)
public class EscudoMixin {
    @Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$escudo(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers,
                                  int luz, int overlay, CallbackInfo ci) {
        if (!stack.is(Items.SHIELD)) {
            return;
        }
        AbstractClientPlayer p = PortadorEscudo.actual();
        if (p == null) {
            return;
        }
        Cosmeticos.Jugador j = Cosmeticos.de(p.getGameProfile().getName());
        String id = j == null ? null : j.de("escudo");
        if (id == null || !"escudo".equals(Cosmeticos3D.hueso(id)) || !Cosmeticos.mostrar(p == Minecraft.getInstance().player)) {
            return;
        }
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);  // igual que el escudo del juego
        pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
        CapaCosmeticos.dibujar(Cosmeticos3D.cubos(id, CapaCosmeticos.estado(p, 0, 0)), pose, buffers, luz);
        pose.popPose();
        ci.cancel();
    }
}
