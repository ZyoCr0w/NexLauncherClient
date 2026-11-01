package com.nexclient;

import com.nexclient.mixin.EntityRendererAccessor;
import com.nexclient.mixin.ShaderGroupAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;

/**
 * Saturación del color del juego: un efecto de pantalla (el «color_convolve» de Minecraft) con su valor de
 * saturación. Al 100 % no se carga nada. Si otro efecto está puesto (por ejemplo, al espectar a un creeper) se respeta.
 */
public final class Saturacion {
    private static final ResourceLocation POST = new ResourceLocation("nexclient", "shaders/post/saturacion.json");
    private static ShaderGroup aplicadoA;
    private static float aplicado = -1f;
    /** Valor con el que no se pudo cargar (para no reintentar cada tick). */
    private static int fallo = -1;

    private Saturacion() {
    }

    public static void tick(Minecraft mc) {
        EntityRenderer er = mc.entityRenderer;
        if (er == null || !OpenGlHelper.shadersSupported) {
            return;
        }
        int valor = Config.get().saturacion;
        float s = valor / 100f;
        ShaderGroup g = er.getShaderGroup();
        boolean nuestro = g != null && POST.toString().equals(g.getShaderGroupName());
        if (valor == 100 || mc.theWorld == null) {
            if (nuestro) {
                er.stopUseShader();
            }
            aplicadoA = null;
            return;
        }
        if (g == null) {
            if (fallo == valor) {
                return;
            }
            try {
                ((EntityRendererAccessor) er).nexclient$cargarEfecto(POST);
            } catch (Throwable e) {
                System.err.println("[NexClient] Saturación no disponible: " + e);
            }
            g = er.getShaderGroup();
            nuestro = g != null && POST.toString().equals(g.getShaderGroupName());
            if (!nuestro) {
                fallo = valor;
                return;
            }
        }
        if (!nuestro) {
            return;
        }
        if (g != aplicadoA || s != aplicado) {
            for (Shader pase : ((ShaderGroupAccessor) g).nexclient$pases()) {
                ShaderUniform u = pase.getShaderManager().getShaderUniform("Saturation");
                if (u != null) {
                    u.set(s);
                }
            }
            aplicadoA = g;
            aplicado = s;
        }
    }
}
