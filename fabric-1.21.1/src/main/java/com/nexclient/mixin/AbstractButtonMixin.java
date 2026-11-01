package com.nexclient.mixin;

import com.nexclient.EstiloNex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Estilo NexClient en todos los botones (opciones, mundos, servidores…). */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin extends AbstractWidget {
    private AbstractButtonMixin(int x, int y, int w, int h, Component texto) {
        super(x, y, w, h, texto);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$dibujar(GuiGraphics g, int mx, int my, float parcial, CallbackInfo ci) {
        if (!EstiloNex.activo()) {
            return;
        }
        ci.cancel();
        int x = getX(), y = getY();
        int color = EstiloNex.boton(g, this, x, y, width, height, isHoveredOrFocused(), active, alpha, -1);
        Component texto = getMessage();
        String es = texto.getString(), traducido = com.nexclient.Tr.t(es);  // NexClient en inglés si el juego lo está
        if (!traducido.equals(es)) {
            texto = Component.literal(traducido);
        }
        renderScrollingString(g, Minecraft.getInstance().font, texto, x + 4, y, x + width - 4, y + height, color);
    }
}
