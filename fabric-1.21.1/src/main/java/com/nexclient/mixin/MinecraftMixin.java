package com.nexclient.mixin;

import com.nexclient.Config;
import com.nexclient.MenuPausaNex;
import com.nexclient.MenuPrincipalNex;
import com.nexclient.PantallaEnvuelta;
import com.nexclient.PantallaLateral;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Menú principal y de pausa de NexClient en lugar de los de Minecraft (o del paquete de texturas). */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen nexclient$menu(Screen pantalla) {
        if (pantalla == null || !Config.get().menuNex) {
            return pantalla;
        }
        if (pantalla instanceof TitleScreen) {
            return new MenuPrincipalNex();
        }
        if (pantalla instanceof PauseScreen && ((PauseScreen) pantalla).showsPauseMenu()) {  // no con F3 + Esc
            return new MenuPausaNex();
        }
        // opciones, multijugador, un jugador…: dentro del panel del menú desde el que se abren
        PantallaLateral raiz = PantallaEnvuelta.raizDe(((Minecraft) (Object) this).screen);
        if (raiz != null && pantalla != raiz && PantallaEnvuelta.envolvible(pantalla)) {
            return new PantallaEnvuelta(raiz, pantalla);
        }
        return pantalla;
    }
}
