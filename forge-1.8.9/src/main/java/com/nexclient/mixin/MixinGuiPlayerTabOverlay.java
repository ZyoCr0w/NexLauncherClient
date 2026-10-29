package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ping en números (con color) en la lista del Tab, en lugar de las barritas. */
@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {
    @Inject(method = "drawPing", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$ping(int ancho, int x, int y, NetworkPlayerInfo info, CallbackInfo ci) {
        if (!Config.get().pingTab) {
            return;
        }
        int ms = info.getResponseTime();
        String texto = String.valueOf(Math.max(0, ms));
        FontRenderer f = Minecraft.getMinecraft().fontRendererObj;
        f.drawStringWithShadow(texto, x + ancho - f.getStringWidth(texto) - 1, y, colorPing(ms));
        ci.cancel();
    }

    /** Un poco más de espacio en cada fila para que quepan los números (las barritas medían 13). */
    @ModifyConstant(method = "renderPlayerlist", constant = @Constant(intValue = 13), require = 0)
    private int nexclient$anchoPing(int valor) {
        return Config.get().pingTab ? valor + 12 : valor;
    }

    private static int colorPing(int ms) {
        if (ms <= 0) {
            return 0xAAAAAA;
        }
        if (ms < 80) {
            return 0x55FF55;
        }
        if (ms < 150) {
            return 0xFFFF55;
        }
        if (ms < 300) {
            return 0xFFAA00;
        }
        return 0xFF5555;
    }
}
