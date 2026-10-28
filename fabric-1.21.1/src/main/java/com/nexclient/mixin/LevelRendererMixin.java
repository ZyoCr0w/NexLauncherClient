package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Contorno del bloque que miras con el color que elijas. */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @ModifyArgs(method = "renderHitOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderShape(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/shapes/VoxelShape;DDDFFFF)V"), require = 0)
    private void nexclient$contorno(Args args) {
        Config c = Config.get();
        if (!c.contorno) {
            return;
        }
        int color = Config.COLORES[c.contornoColor];
        args.set(6, ((color >> 16) & 255) / 255f);
        args.set(7, ((color >> 8) & 255) / 255f);
        args.set(8, (color & 255) / 255f);
        args.set(9, 0.8f);
    }
}
