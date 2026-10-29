package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ping en números (con color) en la lista del Tab, en lugar de las barritas. */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$ping(GuiGraphics g, int ancho, int x, int y, PlayerInfo info, CallbackInfo ci) {
        if (!Config.get().pingTab) {
            return;
        }
        int ms = info.getLatency();
        String texto = String.valueOf(Math.max(0, ms));
        Font f = Minecraft.getInstance().font;
        g.drawString(f, texto, x + ancho - f.width(texto) - 1, y, colorPing(ms), true);
        ci.cancel();
    }

    /** Un poco más de espacio en cada fila para que quepan los números (las barritas medían 13). */
    @ModifyConstant(method = "render", constant = @Constant(intValue = 13), require = 0)
    private int nexclient$anchoPing(int valor) {
        return Config.get().pingTab ? valor + 12 : valor;
    }

    private static int colorPing(int ms) {
        if (ms <= 0) {
            return 0xFFAAAAAA;
        }
        if (ms < 80) {
            return 0xFF55FF55;
        }
        if (ms < 150) {
            return 0xFFFFFF55;
        }
        if (ms < 300) {
            return 0xFFFFAA00;
        }
        return 0xFFFF5555;
    }
}
