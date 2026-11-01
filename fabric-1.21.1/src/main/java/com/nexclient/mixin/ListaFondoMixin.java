package com.nexclient.mixin;

import com.nexclient.EstiloNex;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Listas (mundos, servidores, opciones) con fondo oscuro y líneas finas, sin las texturas del paquete. */
@Mixin(AbstractSelectionList.class)
public abstract class ListaFondoMixin {
    @Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$fondo(GuiGraphics g, CallbackInfo ci) {
        if (EstiloNex.activo()) {
            AbstractWidget w = (AbstractWidget) (Object) this;
            g.fill(w.getX(), w.getY(), w.getRight(), w.getBottom(), 0x60000000);
            ci.cancel();
        }
    }

    @Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$lineas(GuiGraphics g, CallbackInfo ci) {
        if (EstiloNex.activo()) {
            AbstractWidget w = (AbstractWidget) (Object) this;
            g.fill(w.getX(), w.getY() - 1, w.getRight(), w.getY(), 0x33FFFFFF);
            g.fill(w.getX(), w.getBottom(), w.getRight(), w.getBottom() + 1, 0x33FFFFFF);
            ci.cancel();
        }
    }
}
