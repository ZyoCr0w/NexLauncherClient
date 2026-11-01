package com.nexclient.mixin;

import com.nexclient.Cps;
import com.nexclient.Efectos;
import com.nexclient.Movimiento;
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
    /** Con el zoom puesto, la rueda cambia el aumento en vez de la ranura de la barra. */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$ruedaZoom(long ventana, double dx, double dy, CallbackInfo ci) {
        if (Movimiento.zoomActivo && Minecraft.getInstance().screen == null) {
            Movimiento.rueda(dy);
            ci.cancel();
        }
    }

    /**
     * Ranuras de la barra puestas en un botón del ratón (los laterales, por ejemplo): se cambia de objeto en cuanto
     * llega el clic, también caminando o agachado.
     */
    private static void ranura(Minecraft mc, int boton) {
        if (mc.player == null || mc.player.isSpectator()) {
            return;
        }
        net.minecraft.client.KeyMapping[] barra = mc.options.keyHotbarSlots;
        for (int i = 0; i < barra.length; i++) {
            com.mojang.blaze3d.platform.InputConstants.Key k =
                    net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(barra[i]);
            if (k.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE && k.getValue() == boton) {
                boolean guardarBarra = mc.player.isCreative()
                        && (mc.options.keySaveHotbarActivator.isDown() || mc.options.keyLoadHotbarActivator.isDown());
                if (!guardarBarra) {  // en creativo, C/X + número guarda o carga una barra: eso lo hace el juego
                    mc.player.getInventory().selected = i;
                }
                return;
            }
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void nexclient$clic(long ventana, int boton, int accion, int mods, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (accion == GLFW.GLFW_PRESS && mc.screen == null) {
            ranura(mc, boton);
            Cps.clic(boton);
            if (boton == 0) {
                Efectos.clicIzquierdo(mc);
            }
        }
    }
}
