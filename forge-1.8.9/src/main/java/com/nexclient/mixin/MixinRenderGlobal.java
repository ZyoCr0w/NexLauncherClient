package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Contorno del bloque que miras: color y grosor propios. */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @Redirect(method = "drawSelectionBox", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"), require = 0)
    private void nexclient$color(float r, float g, float b, float a) {
        Config c = Config.get();
        if (!c.contorno) {
            GlStateManager.color(r, g, b, a);
            return;
        }
        int color = Config.COLORES[c.contornoColor];
        GlStateManager.color(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, 0.8f);
    }

    @Redirect(method = "drawSelectionBox", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLineWidth(F)V"),
            require = 0)
    private void nexclient$grosor(float ancho) {
        Config c = Config.get();
        GL11.glLineWidth(c.contorno ? c.contornoGrosor : ancho);
    }
}
