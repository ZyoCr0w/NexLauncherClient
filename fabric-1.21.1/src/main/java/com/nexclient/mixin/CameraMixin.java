package com.nexclient.mixin;

import com.nexclient.VistaLibre;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vista libre: la cámara en tercera persona usa su propia rotación, no la del jugador. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"),
            require = 0)
    private void nexclient$vistaLibre(BlockGetter nivel, Entity entidad, boolean separada, boolean invertida,
                                      float parcial, CallbackInfo ci) {
        if (VistaLibre.activa && separada) {
            setRotation(VistaLibre.yaw, VistaLibre.pitch);
        }
    }
}
