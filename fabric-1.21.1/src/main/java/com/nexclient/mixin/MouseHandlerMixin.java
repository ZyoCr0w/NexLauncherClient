package com.nexclient.mixin;

import com.nexclient.Cps;
import com.nexclient.Efectos;
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
        Minecraft mc = Minecraft.getInstance();
        if (accion == GLFW.GLFW_PRESS && mc.screen == null) {
            Cps.clic(boton);
            if (boton == 0) {
                Efectos.clicIzquierdo(mc);
            }
        }
    }
}
