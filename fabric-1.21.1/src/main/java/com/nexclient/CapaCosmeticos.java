package com.nexclient;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.List;

/** Cosméticos 3D sobre el jugador (cabeza, espalda, cuerpo, auras, mascotas y mano), dibujados con cajas de color. */
public class CapaCosmeticos extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final String[] CATEGORIAS = {"cabeza", "espalda", "cuerpo", "capa", "aura", "mascota", "mano"};
    private static final int[][] CARAS = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};
    private static ResourceLocation blanca;

    public CapaCosmeticos(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
        super(padre);
    }

    /** Textura blanca de 16 × 16 (las cajas llevan el color en cada vértice). */
    public static ResourceLocation blanca() {
        if (blanca == null) {
            NativeImage img = new NativeImage(16, 16, false);
            img.fillRect(0, 0, 16, 16, 0xFFFFFFFF);
            blanca = ResourceLocation.fromNamespaceAndPath("nexclient", "blanca");
            Minecraft.getInstance().getTextureManager().register(blanca, new DynamicTexture(img));
        }
        return blanca;
    }

    public static Cosmeticos3D.Estado estado(AbstractClientPlayer p, float mov, float parcial) {
        Cosmeticos3D.Estado e = new Cosmeticos3D.Estado();
        e.t = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000f;
        e.mov = Math.max(0f, Math.min(1f, mov));
        e.aire = (!p.onGround() || p.isFallFlying()) && !p.isPassenger() ? 1f : 0f;
        e.agachado = p.isCrouching() ? 1f : 0f;
        e.golpe = p.getAttackAnim(parcial);
        return e;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer p, float limbSwing,
                       float limbSwingAmount, float parcial, float edad, float yawCabeza, float pitchCabeza) {
        Minecraft mc = Minecraft.getInstance();
        Cosmeticos.Jugador j = Cosmeticos.de(p.getGameProfile().getName());
        if (j == null || p.isInvisible() || !Cosmeticos.mostrar(p == mc.player)) {
            return;
        }
        Cosmeticos3D.Estado e = null;
        for (String cat : CATEGORIAS) {
            String id = j.de(cat);
            String hueso = Cosmeticos3D.hueso(id);
            if (hueso == null || hueso.equals("escudo")) {
                continue;
            }
            if (e == null) {
                e = estado(p, limbSwingAmount, parcial);
            }
            pose.pushPose();
            if (hueso.equals("cabeza")) {
                getParentModel().head.translateAndRotate(pose);
            } else if (hueso.equals("cuerpo")) {
                getParentModel().body.translateAndRotate(pose);
            } else if (hueso.equals("mano")) {  // lo que se lleva en la mano izquierda (maracas, bastón…)
                getParentModel().leftArm.translateAndRotate(pose);
            }
            pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
            dibujar(Cosmeticos3D.cubos(id, e), pose, buffers, luz);
            pose.popPose();
        }
    }

    /** Dibuja cajas en el espacio actual (unidades en píxeles). */
    public static void dibujar(List<Cosmeticos3D.Cubo> cubos, PoseStack pose, MultiBufferSource buffers, int luz) {
        if (cubos.isEmpty()) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(blanca()));
        PoseStack.Pose ultima = pose.last();
        Matrix4f m = ultima.pose();
        for (Cosmeticos3D.Cubo c : cubos) {
            float[][] p = c.esquinas;
            float cx = 0, cy = 0, cz = 0;
            for (float[] q : p) {
                cx += q[0] / 8;
                cy += q[1] / 8;
                cz += q[2] / 8;
            }
            int l = c.brillo ? LightTexture.FULL_BRIGHT : luz;
            for (int[] f : CARAS) {
                float[] a = p[f[0]], b = p[f[1]], d = p[f[3]];
                float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
                float vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
                float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
                float fx = (a[0] + p[f[2]][0]) / 2 - cx, fy = (a[1] + p[f[2]][1]) / 2 - cy, fz = (a[2] + p[f[2]][2]) / 2 - cz;
                boolean adentro = nx * fx + ny * fy + nz * fz < 0;  // en espejo las caras quedan al revés
                float largo = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (largo < 1e-6f) {
                    continue;
                }
                float s = adentro ? -1f / largo : 1f / largo;
                int[] orden = adentro ? new int[]{f[0], f[3], f[2], f[1]} : f;
                for (int k = 0; k < 4; k++) {
                    float[] v = p[orden[k]];
                    vc.addVertex(m, v[0], v[1], v[2]).setColor(c.r, c.g, c.b, c.a).setUv(k == 1 || k == 2 ? 1 : 0, k >= 2 ? 1 : 0)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(l).setNormal(ultima, nx * s, ny * s, nz * s);
                }
            }
        }
    }
}
