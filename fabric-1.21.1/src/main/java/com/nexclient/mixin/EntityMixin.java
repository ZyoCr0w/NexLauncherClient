package com.nexclient.mixin;

import com.nexclient.VistaLibre;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Con la vista libre, el ratón gira la cámara y no a tu personaje. */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$vistaLibre(double dx, double dy, CallbackInfo ci) {
        if (VistaLibre.activa && (Object) this == Minecraft.getInstance().player) {
            VistaLibre.girar(dx, dy);
            ci.cancel();
        }
    }
}
