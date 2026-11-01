package com.nexclient;

import com.nexclient.mixin.GameRendererAccessor;
import com.nexclient.mixin.PostChainAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

/**
 * Saturación del color del juego: un efecto de pantalla (el «color_convolve» de Minecraft) con su valor de
 * saturación. Al 100 % no se carga nada. Si otro efecto está puesto (por ejemplo, al espectar a un creeper) se respeta.
 */
public final class Saturacion {
    private static final ResourceLocation POST = ResourceLocation.fromNamespaceAndPath("nexclient", "shaders/post/saturacion.json");
    private static PostChain aplicadoA;
    private static float aplicado = -1f;
    /** Valor con el que no se pudo cargar (para no reintentar cada tick). */
    private static int fallo = -1;

    private Saturacion() {
    }

    public static void tick(Minecraft mc) {
        GameRenderer gr = mc.gameRenderer;
        if (gr == null) {
            return;
        }
        int valor = Config.get().saturacion;
        float s = valor / 100f;
        PostChain g = gr.currentEffect();
        boolean nuestro = g != null && POST.toString().equals(g.getName());
        if (valor == 100 || mc.level == null) {
            if (nuestro) {
                gr.shutdownEffect();
            }
            aplicadoA = null;
            return;
        }
        if (g == null) {
            if (fallo == valor) {
                return;
            }
            try {
                ((GameRendererAccessor) gr).nexclient$cargarEfecto(POST);
            } catch (Throwable e) {
                System.err.println("[NexClient] Saturación no disponible: " + e);
            }
            g = gr.currentEffect();
            nuestro = g != null && POST.toString().equals(g.getName());
            if (!nuestro) {
                fallo = valor;
                return;
            }
        }
        if (!nuestro) {
            return;
        }
        if (g != aplicadoA || s != aplicado) {
            for (PostPass pase : ((PostChainAccessor) g).nexclient$pases()) {
                pase.getEffect().safeGetUniform("Saturation").set(s);
            }
            aplicadoA = g;
            aplicado = s;
        }
    }
}
