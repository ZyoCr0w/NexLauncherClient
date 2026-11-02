package com.nexclient;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import org.joml.Matrix4f;

/** El logo de NexLauncher encima del nombre (solo en el mundo, no en el Tab). */
public final class Etiquetas {
    /** Alto de una línea de nombre en el mundo, igual que la del marcador «debajo del nombre». */
    private static final float LINEA = 9.0F * 1.15F * 0.025F;

    /** Falso mientras el inventario dibuja a tu personaje (apaga las sombras). */
    public static volatile boolean enMundo = true;

    private Etiquetas() {
    }

    /** ¿Se dibuja tu propio nombre? En tercera persona (F5), no en el inventario ni con la interfaz oculta (F1). */
    public static boolean nombrePropio(net.minecraft.world.entity.Entity e) {
        Minecraft mc = Minecraft.getInstance();
        return e != null && e == mc.player && e == mc.getCameraEntity() && Config.get().nombrePropio && enMundo
                && !mc.options.getCameraType().isFirstPerson() && Minecraft.renderNames()
                && !e.isInvisible() && !e.isVehicle();
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

    /** Texturas del logo de NexLauncher para la insignia (la del creador trae 24 cuadros, uno debajo de otro). */
    private static final ResourceLocation GRIS = ResourceLocation.fromNamespaceAndPath("nexclient", "textures/gui/insignia_gris.png");
    private static final ResourceLocation VERDE = ResourceLocation.fromNamespaceAndPath("nexclient", "textures/gui/insignia_verde.png");
    private static final ResourceLocation ARCOIRIS = ResourceLocation.fromNamespaceAndPath("nexclient", "textures/gui/insignia_arcoiris.png");
    private static final int CUADROS = 24;
    /** Tamaño del logo, en píxeles del texto de los nombres (el nombre mide 8 de alto). */
    private static final float TAM = 12f;

    /**
     * Se llama justo después de que el juego dibuja el nombre del jugador (si no lo dibujó, no se llama): el logo de
     * NexLauncher encima, gris si juega offline, verde si es premium y en arcoíris (animado) si es el creador.
     */
    public static void dibujar(AbstractClientPlayer p, PoseStack pose, MultiBufferSource buffers, int luz, float parcial) {
        Minecraft mc = Minecraft.getInstance();
        if (!Config.get().etiquetas || mc.level == null) {
            return;
        }
        Cosmeticos.Jugador j = Cosmeticos.de(p.getGameProfile().getName());
        if (j == null) {
            return;
        }
        if (j.insignia == null) {  // NexSkins todavía sin insignias: la línea de texto de antes
            dibujarTexto(p, j, pose, buffers, luz, parcial);
            return;
        }
        double d = mc.getEntityRenderDispatcher().distanceToSqr(p);
        if (d > 1024.0) {
            return;
        }
        Vec3 v = p.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, p.getViewYRot(parcial));
        if (v == null) {
            return;
        }
        boolean marcador = d < 100.0 && p.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null;
        float dy = marcador ? LINEA * 2 : LINEA;
        boolean discreto = p.isDiscrete();
        ResourceLocation tex = "creador".equals(j.insignia) ? ARCOIRIS : "premium".equals(j.insignia) ? VERDE : GRIS;
        float v0 = 0, v1 = 1;
        if (tex == ARCOIRIS) {
            int k = (int) ((System.currentTimeMillis() / 70L) % CUADROS);
            v0 = k / (float) CUADROS;
            v1 = (k + 1) / (float) CUADROS;
        }
        pose.pushPose();
        pose.translate(v.x, v.y + 0.5 + dy, v.z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025F, -0.025F, 0.025F);
        Matrix4f m = pose.last().pose();
        float x0 = -TAM / 2, x1 = TAM / 2, y0 = 8 - TAM, y1 = 8;
        if (!discreto) {  // a través de las paredes, tenue (como el nombre)
            cuadro(buffers.getBuffer(RenderType.textSeeThrough(tex)), m, x0, y0, x1, y1, v0, v1, 0x40, luz);
        }
        cuadro(buffers.getBuffer(RenderType.text(tex)), m, x0, y0, x1, y1, v0, v1, discreto ? 0x80 : 0xFF, luz);
        pose.popPose();
    }

    private static void cuadro(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float v0, float v1,
                               int alfa, int luz) {
        // mismo orden que las letras del juego (así se ve de frente con cualquier tipo de dibujo)
        vc.addVertex(m, x0, y0, 0).setColor(255, 255, 255, alfa).setUv(0, v0).setLight(luz);
        vc.addVertex(m, x0, y1, 0).setColor(255, 255, 255, alfa).setUv(0, v1).setLight(luz);
        vc.addVertex(m, x1, y1, 0).setColor(255, 255, 255, alfa).setUv(1, v1).setLight(luz);
        vc.addVertex(m, x1, y0, 0).setColor(255, 255, 255, alfa).setUv(1, v0).setLight(luz);
    }

    /** La línea de texto de antes (por si NexSkins aún no manda la insignia). */
    private static void dibujarTexto(AbstractClientPlayer p, Cosmeticos.Jugador j, PoseStack pose, MultiBufferSource buffers,
                                     int luz, float parcial) {
        Minecraft mc = Minecraft.getInstance();
        Component texto = texto(j);
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
