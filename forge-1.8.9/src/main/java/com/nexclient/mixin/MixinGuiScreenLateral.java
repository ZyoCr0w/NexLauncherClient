package com.nexclient.mixin;

import com.nexclient.Lateral;
import com.nexclient.MenuLateral;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pantallas de Minecraft dentro del panel de NexClient: más angostas y con el ratón corrido. */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenLateral {
    @ModifyVariable(method = "setWorldAndResolution", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int nexclient$ancho(int ancho) {
        return Lateral.es(this) ? Math.max(50, ancho - MenuLateral.ANCHO) : ancho;
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventX()I",
            remap = false), require = 0)
    private int nexclient$ratonX() {
        int x = Mouse.getEventX();
        return Lateral.es(this) ? Lateral.eventoX((GuiScreen) (Object) this, x) : x;
    }
}
