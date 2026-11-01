package com.nexclient.mixin;

import com.nexclient.EstiloNex;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Listas (mundos, servidores, paquetes de recursos) sin la tierra: fondo oscuro y transparente. */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlot {
    @Shadow public int width;
    @Shadow public int height;
    @Shadow public int top;
    @Shadow public int bottom;
    @Shadow public int left;
    @Shadow public int right;

    @Inject(method = "overlayBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$franja(int y1, int y2, int alfa1, int alfa2, CallbackInfo ci) {
        if (EstiloNex.activo()) {
            EstiloNex.franja(left, y1, right, y2);
            ci.cancel();
        }
    }

    @Inject(method = "drawContainerBackground", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void nexclient$fondoLista(Tessellator t, CallbackInfo ci) {
        if (EstiloNex.activo()) {
            net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, 0x70000000);
            ci.cancel();
        }
    }
}
