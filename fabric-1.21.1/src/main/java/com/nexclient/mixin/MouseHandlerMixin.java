package com.nexclient.mixin;

import com.nexclient.Cps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cuenta cada clic (solo jugando, no en menús) para el contador de CPS. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void nexclient$clic(long ventana, int boton, int accion, int mods, CallbackInfo ci) {
        if (accion == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            Cps.clic(boton);
        }
    }
}
