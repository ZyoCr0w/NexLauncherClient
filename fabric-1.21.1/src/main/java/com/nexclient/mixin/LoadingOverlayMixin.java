package com.nexclient.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.InputStream;
import java.util.function.IntSupplier;

/** Pantalla de carga de NexClient: fondo oscuro y el logo con «NexClient · Cargando…» (la barra es la de siempre). */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Unique
    private static ResourceLocation nexclient$imagen;

    /** La imagen se lee del .jar a mano: durante la primera carga el juego aún no tiene los recursos de los mods. */
    @Unique
    private static ResourceLocation nexclient$imagen() {
        if (nexclient$imagen == null) {
            nexclient$imagen = ResourceLocation.fromNamespaceAndPath("nexclient", "carga_propia");
            try (InputStream in = LoadingOverlayMixin.class.getResourceAsStream("/assets/nexclient/textures/gui/cargando.png")) {
                if (in != null) {
                    Minecraft.getInstance().getTextureManager().register(nexclient$imagen, new DynamicTexture(NativeImage.read(in)));
                }
            } catch (Exception e) {
                // sin imagen: queda vacío
            }
        }
        return nexclient$imagen;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"), require = 0)
    private int nexclient$fondo(IntSupplier original) {
        return 0x0B0B10;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 0, target =
            "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), require = 0)
    private void nexclient$logo(GuiGraphics g, ResourceLocation logo, int x, int y, int w, int h, float u, float v,
                                int uw, int vh, int tw, int th) {
        g.blit(nexclient$imagen(), x, y, w * 2, h, 0, 0, 1024, 256, 1024, 256);  // las dos mitades del logo de Mojang
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 1, target =
            "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), require = 0)
    private void nexclient$logo2(GuiGraphics g, ResourceLocation logo, int x, int y, int w, int h, float u, float v,
                                 int uw, int vh, int tw, int th) {
        // ya va en la primera
    }
}
