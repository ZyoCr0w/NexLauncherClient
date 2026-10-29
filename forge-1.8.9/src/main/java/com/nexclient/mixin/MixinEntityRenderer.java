package com.nexclient.mixin;

import com.nexclient.Config;
import com.nexclient.Movimiento;
import com.nexclient.VistaLibre;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom (campo de visión) y vista libre (la cámara usa su propia rotación en tercera persona). */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"),
            require = 0)
    private float nexclient$yaw(Entity e) {
        return VistaLibre.activa ? VistaLibre.yaw : e.rotationYaw;
    }

    @Redirect(method = "orientCamera",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F"), require = 0)
    private float nexclient$yawPrevio(Entity e) {
        return VistaLibre.activa ? VistaLibre.yaw : e.prevRotationYaw;
    }

    @Redirect(method = "orientCamera",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"), require = 0)
    private float nexclient$pitch(Entity e) {
        return VistaLibre.activa ? VistaLibre.pitch : e.rotationPitch;
    }

    @Redirect(method = "orientCamera",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F"), require = 0)
    private float nexclient$pitchPrevio(Entity e) {
        return VistaLibre.activa ? VistaLibre.pitch : e.prevRotationPitch;
    }

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true, require = 0)
    private void nexclient$zoom(float parcial, boolean usarAjuste, CallbackInfoReturnable<Float> cir) {
        if (Movimiento.zoomActivo) {
            cir.setReturnValue(cir.getReturnValue() / Config.get().zoomFactor);
        }
    }
}
