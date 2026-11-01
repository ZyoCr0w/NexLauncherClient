package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Cosméticos 3D sobre el jugador (cabeza, espalda, cuerpo y auras), dibujados con cajas de color. En 1.8.9 no hay
 * escudos: el escudo elegido se ve en el costado del brazo izquierdo (en 1.8.9 no hay escudos), solo como adorno.
 */
public class CapaCosmeticos implements LayerRenderer<AbstractClientPlayer> {
    private static final String[] CATEGORIAS = {"cabeza", "espalda", "cuerpo", "capa", "aura", "escudo"};
    private static final int[][] CARAS = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};
    private final RenderPlayer render;

    public CapaCosmeticos(RenderPlayer render) {
        this.render = render;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float parcial, float edad,
                              float yawCabeza, float pitchCabeza, float escala) {
        Minecraft mc = Minecraft.getMinecraft();
        Cosmeticos.Jugador j = Cosmeticos.de(p.getName());
        if (j == null || p.isInvisible() || !Cosmeticos.mostrar(p == mc.thePlayer)) {
            return;
        }
        Cosmeticos3D.Estado e = null;
        for (String cat : CATEGORIAS) {
            String id = j.de(cat);
            String hueso = Cosmeticos3D.hueso(id);
            if (hueso == null) {
                continue;
            }
            if (e == null) {
                e = new Cosmeticos3D.Estado();
                e.t = (System.nanoTime() / 1000000L % 3600000L) / 1000f;
                e.mov = Math.max(0f, Math.min(1f, limbSwingAmount));
                e.aire = !p.onGround && p.ridingEntity == null ? 1f : 0f;
                e.agachado = p.isSneaking() ? 1f : 0f;
                e.golpe = p.getSwingProgress(parcial);
            }
            GlStateManager.pushMatrix();
            if (p.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }
            if (hueso.equals("cabeza")) {
                render.getMainModel().bipedHead.postRender(0.0625F);
            } else if (hueso.equals("escudo")) {
                render.getMainModel().bipedLeftArm.postRender(0.0625F);
            } else if (hueso.equals("cuerpo")) {
                render.getMainModel().bipedBody.postRender(0.0625F);
            }
            GlStateManager.scale(0.0625F, 0.0625F, 0.0625F);
            if (hueso.equals("escudo")) {  // en el costado de afuera del brazo izquierdo, a la altura de la mano
                GlStateManager.translate(2.6F, 5.8F, 0F);
                GlStateManager.rotate(-90F, 0F, 1F, 0F);  // el frente del escudo (-Z) mira hacia afuera (+X)
                GlStateManager.scale(0.55F, 0.55F, 0.55F);
            }
            dibujar(Cosmeticos3D.cubos(id, e));
            GlStateManager.popMatrix();
        }
    }

    /** Dibuja cajas en el espacio actual (unidades en píxeles), con la luz horneada en cada cara. */
    public static void dibujar(List<Cosmeticos3D.Cubo> cubos) {
        if (cubos.isEmpty()) {
            return;
        }
        float bx = OpenGlHelper.lastBrightnessX, by = OpenGlHelper.lastBrightnessY;
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        for (int pasada = 0; pasada < 2; pasada++) {  // 0: normales; 1: las que brillan (luz al máximo)
            if (pasada == 1) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            }
            Tessellator t = Tessellator.getInstance();
            WorldRenderer w = t.getWorldRenderer();
            w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            boolean hay = false;
            for (Cosmeticos3D.Cubo c : cubos) {
                if (c.brillo != (pasada == 1)) {
                    continue;
                }
                hay = true;
                float[][] p = c.esquinas;
                for (int[] f : CARAS) {
                    float[] a = p[f[0]], b = p[f[1]], d = p[f[3]];
                    float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
                    float vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
                    float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
                    float largo = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (largo < 1e-6f) {
                        continue;
                    }
                    // luz como la de Minecraft: arriba 1.0, abajo 0.5, frente/atrás 0.8, lados 0.6 (Y va hacia abajo)
                    float ax = Math.abs(nx) / largo, ay = Math.abs(ny) / largo, az = Math.abs(nz) / largo;
                    float luz = c.brillo ? 1f : ay >= ax && ay >= az ? 0.75f + 0.25f * Math.signum(-ny) : az >= ax ? 0.8f : 0.6f;
                    for (int k : f) {
                        w.pos(p[k][0], p[k][1], p[k][2]).color(c.r * luz, c.g * luz, c.b * luz, c.a).endVertex();
                    }
                }
            }
            if (hay) {
                t.draw();
            } else {
                w.finishDrawing();
            }
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
