package com.nexclient.mixin;

import com.nexclient.Config;
import com.nexclient.Efectos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animación de blockhit de 1.7: al bloquear con espada, el golpe se sigue viendo. */
@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    private static float nexclient$parcial;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void nexclient$guardarParcial(float parcial, CallbackInfo ci) {
        nexclient$parcial = parcial;
    }

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$fuego(float parcial, CallbackInfo ci) {
        if (Config.get().ocultarFuego) {
            ci.cancel();
        }
    }

    @Inject(method = "transformFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void nexclient$transformar(float equipado, float golpe, CallbackInfo ci) {
        Config c = Config.get();
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        boolean espada = c.blockhit && c.estilo17 && p != null && p.isBlocking();
        boolean comida = c.blockhitComida && Efectos.comiendo();
        if (p == null || (!espada && !comida)) {
            return;
        }
        // Igual que el método original, pero con el golpe real en vez de 0
        float s = p.getSwingProgress(nexclient$parcial);
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipado * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(s * s * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(s) * (float) Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        ci.cancel();
    }
}
