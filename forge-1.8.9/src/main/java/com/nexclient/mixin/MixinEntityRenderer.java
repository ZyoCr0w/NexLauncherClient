package com.nexclient.mixin;

import com.nexclient.Config;
import com.nexclient.Movimiento;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: reduce el campo de visión mientras se mantiene la tecla. */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true, require = 0)
    private void nexclient$zoom(float parcial, boolean usarAjuste, CallbackInfoReturnable<Float> cir) {
        if (Movimiento.zoomActivo) {
            cir.setReturnValue(cir.getReturnValue() / Config.get().zoomFactor);
        }
    }
}
