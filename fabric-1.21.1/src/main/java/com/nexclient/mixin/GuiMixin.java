package com.nexclient.mixin;

import com.nexclient.Config;
import com.nexclient.Hud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mira personalizada en lugar de la del juego (solo en primera persona y sin F3). */
@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$mira(GuiGraphics g, DeltaTracker tiempo, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!Config.get().mira || !mc.options.getCameraType().isFirstPerson() || mc.getDebugOverlay().showDebugScreen()
                || mc.options.hideGui) {
            return;
        }
        Hud.mira(g, g.guiWidth() / 2, g.guiHeight() / 2);
        ci.cancel();
    }
}
