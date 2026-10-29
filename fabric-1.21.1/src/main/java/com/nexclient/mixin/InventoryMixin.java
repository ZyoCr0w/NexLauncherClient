package com.nexclient.mixin;

import com.nexclient.Movimiento;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Respaldo: con el zoom puesto, la rueda nunca cambia de ranura (aunque otro mod se adelante en onScroll). */
@Mixin(Inventory.class)
public class InventoryMixin {
    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$sinCambiarRanura(double pasos, CallbackInfo ci) {
        if (Movimiento.zoomActivo) {
            Movimiento.rueda(pasos);
            ci.cancel();
        }
    }
}
