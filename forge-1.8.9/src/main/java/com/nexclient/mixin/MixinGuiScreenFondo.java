package com.nexclient.mixin;

import com.nexclient.EstiloNex;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fondo oscuro en lugar de la tierra en las pantallas sin mundo detrás. */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenFondo {
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$fondo(int tinte, CallbackInfo ci) {
        if (EstiloNex.activo()) {
            if (!com.nexclient.Lateral.es(this)) {  // en el panel se ve el fondo animado de NexClient
                EstiloNex.fondo(width, height);
            }
            ci.cancel();
        }
    }

    @Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$fondoMundo(int tinte, CallbackInfo ci) {
        if (com.nexclient.Lateral.es(this)) {  // el panel ya oscurece el juego
            ci.cancel();
        }
    }
}
