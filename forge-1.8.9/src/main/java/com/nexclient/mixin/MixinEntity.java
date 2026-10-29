package com.nexclient.mixin;

import com.nexclient.VistaLibre;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Con la vista libre, el ratón gira la cámara y no a tu personaje. */
@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$vistaLibre(float dx, float dy, CallbackInfo ci) {
        if (VistaLibre.activa && (Object) this == Minecraft.getMinecraft().thePlayer) {
            VistaLibre.girar(dx, dy);
            ci.cancel();
        }
    }
}
