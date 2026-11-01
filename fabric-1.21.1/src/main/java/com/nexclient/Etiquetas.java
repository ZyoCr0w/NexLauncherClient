package com.nexclient;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import org.joml.Matrix4f;

/** La línea de NexLauncher encima del nombre (solo en el mundo, no en el Tab). */
public final class Etiquetas {
    /** Alto de una línea de nombre en el mundo, igual que la del marcador «debajo del nombre». */
    private static final float LINEA = 9.0F * 1.15F * 0.025F;

    private Etiquetas() {
    }

    public static Component texto(Cosmeticos.Jugador j) {
        if (j == null || j.etiqueta == null) {
            return null;
        }
        Object c = j.cache.get("etiqueta");
        if (c == null) {
            Minecraft mc = Minecraft.getInstance();
            try {
                c = Component.Serializer.fromJson(j.etiqueta, mc.level.registryAccess());
            } catch (Exception e) {
                c = Component.literal("NEXLAUNCHER").withStyle(net.minecraft.ChatFormatting.GRAY);
            }
            if (c == null) {
                c = Component.empty();
            }
            j.cache.put("etiqueta", c);
        }
        return (Component) c;
    }

    /** Se llama justo después de que el juego dibuja el nombre del jugador (si no lo dibujó, no se llama). */
    public static void dibujar(AbstractClientPlayer p, PoseStack pose, MultiBufferSource buffers, int luz, float parcial) {
        Minecraft mc = Minecraft.getInstance();
        if (!Config.get().etiquetas || mc.level == null) {
            return;
        }
        Component texto = texto(Cosmeticos.de(p.getGameProfile().getName()));
        if (texto == null || texto.getString().isEmpty()) {
            return;
        }
        double d = mc.getEntityRenderDispatcher().distanceToSqr(p);
        if (d > 1024.0) {  // 32 bloques: de más lejos no se alcanza a leer
            return;
        }
        Vec3 v = p.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, p.getViewYRot(parcial));
        if (v == null) {
            return;
        }
        // Encima del nombre; si el servidor pone una línea debajo (vida, nivel…), el nombre sube y la nuestra también
        boolean marcador = d < 100.0 && p.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null;
        float dy = marcador ? LINEA * 2 : LINEA;  // encima del nombre (y encima del marcador, si hay)
        boolean discreto = p.isDiscrete();
        pose.pushPose();
        pose.translate(v.x, v.y + 0.5 + dy, v.z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025F, -0.025F, 0.025F);
        pose.scale(0.8F, 0.8F, 0.8F);  // un poco más chica que el nombre
        Matrix4f m = pose.last().pose();
        Font f = mc.font;
        float x = -f.width(texto) / 2f;
        int fondo = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        f.drawInBatch(texto, x, 0, 0x20FFFFFF, false, m, buffers,
                discreto ? Font.DisplayMode.NORMAL : Font.DisplayMode.SEE_THROUGH, fondo, luz);
        if (!discreto) {
            f.drawInBatch(texto, x, 0, 0xFFFFFFFF, false, m, buffers, Font.DisplayMode.NORMAL, 0, luz);
        }
        pose.popPose();
    }
}
