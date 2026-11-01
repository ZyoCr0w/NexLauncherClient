package com.nexclient;

import net.minecraft.client.Minecraft;

/** Lo pone MixinGuiButton en todos los botones: llama al mouseDragged del juego (mueve los deslizadores). */
public interface ArrastreBoton {
    void nexclient$arrastrar(Minecraft mc, int mx, int my);
}
