package com.nexclient.mixin;

import com.nexclient.EstiloNex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fondo oscuro y transparente en las pantallas (sin panorama, tierra ni desenfoque del paquete de texturas). */
@Mixin(Screen.class)
public abstract class ScreenFondoMixin {
    @Shadow
    protected Minecraft minecraft;
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$fondo(GuiGraphics g, int mx, int my, float parcial, CallbackInfo ci) {
        if (com.nexclient.PantallaEnvuelta.HIJAS.contains((Object) this)) {  // dentro del panel: se ve su fondo
            ci.cancel();
            return;
        }
        if (EstiloNex.activo() && minecraft != null) {
            EstiloNex.fondo(g, width, height, minecraft.level != null);
            ci.cancel();
        }
    }
}
