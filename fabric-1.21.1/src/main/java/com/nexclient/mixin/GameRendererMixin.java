package com.nexclient.mixin;

import com.nexclient.Movimiento;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: reduce el campo de visión mientras se mantiene la tecla. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void nexclient$zoom(Camera camara, float parcial, boolean usarAjuste, CallbackInfoReturnable<Double> cir) {
        if (Movimiento.hayZoom()) {
            cir.setReturnValue(Movimiento.fov(cir.getReturnValue()));
        }
    }
}
