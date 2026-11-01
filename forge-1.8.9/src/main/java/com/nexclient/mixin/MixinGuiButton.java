package com.nexclient.mixin;

import com.nexclient.EstiloNex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Estilo NexClient en todos los botones y deslizadores (opciones, mundos, servidores…). */
@Mixin(GuiButton.class)
public abstract class MixinGuiButton {
    @Shadow public boolean visible;
    @Shadow protected boolean hovered;
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public int width;
    @Shadow public int height;

    @Shadow
    protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$dibujar(final Minecraft mc, final int mx, final int my, CallbackInfo ci) {
        if (!EstiloNex.activo()) {
            return;
        }
        ci.cancel();
        if (!visible) {
            return;
        }
        hovered = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        EstiloNex.boton((GuiButton) (Object) this, mc, mx, my, new Runnable() {
            public void run() {
                mouseDragged(mc, mx, my);
            }
        });
    }
}
