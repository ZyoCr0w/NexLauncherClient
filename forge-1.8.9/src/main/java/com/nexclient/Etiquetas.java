package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IChatComponent;
import org.lwjgl.opengl.GL11;

/** La línea de NexLauncher encima del nombre (solo en el mundo, no en el Tab). */
public final class Etiquetas {
    private Etiquetas() {
    }

    /** El JSON de la etiqueta convertido a texto con códigos de color (§), guardado para no convertirlo cada vez. */
    public static String texto(Cosmeticos.Jugador j) {
        if (j == null || j.etiqueta == null) {
            return null;
        }
        Object c = j.cache.get("etiqueta");
        if (c == null) {
            try {
                IChatComponent t = IChatComponent.Serializer.jsonToComponent(j.etiqueta);
                c = t == null ? "" : t.getFormattedText();
            } catch (Exception e) {
                c = "§7NEXLAUNCHER";
            }
            j.cache.put("etiqueta", c);
        }
        return (String) c;
    }

    /** ¿Se dibuja tu propio nombre? En tercera persona (F5), no en el inventario ni con la interfaz oculta (F1). */
    public static boolean nombrePropio(net.minecraft.entity.Entity e) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        return e != null && e == mc.thePlayer && e == rm.livingPlayer && Config.get().nombrePropio
                && mc.gameSettings.thirdPersonView != 0 && rm.isRenderShadow() && Minecraft.isGuiEnabled()
                && !e.isInvisible() && e.riddenByEntity == null;
    }

    /**
     * Se llama después de que el juego dibuja lo de encima de la cabeza (RenderLivingEvent.Specials.Post). Solo se
     * dibuja si el nombre también se ve: sin agacharse, no invisible, sin montar nada y a menos de 32 bloques.
     */
    public static void dibujar(AbstractClientPlayer p, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Config.get().etiquetas || (p == mc.getRenderViewEntity() && !nombrePropio(p)) || p.isSneaking() || p.riddenByEntity != null
                || p.isInvisibleToPlayer(mc.thePlayer) || !Minecraft.isGuiEnabled()) {
            return;
        }
        String s = texto(Cosmeticos.de(p.getName()));
        if (s == null || s.isEmpty()) {
            return;
        }
        RenderManager rm = mc.getRenderManager();
        double d = p.getDistanceSqToEntity(rm.livingPlayer);
        if (d > 1024.0) {
            return;
        }
        FontRenderer f = mc.fontRendererObj;
        float escala = 0.016666668F * 1.6F;
        float linea = f.FONT_HEIGHT * 1.15F * escala;
        // Encima del nombre; si el servidor pone una línea debajo (vida, nivel…), el nombre sube y la nuestra también
        boolean marcador = d < 100.0 && p.getWorldScoreboard().getObjectiveInDisplaySlot(2) != null;
        float dy = marcador ? linea * 2 : linea;  // encima del nombre (y encima del marcador, si hay)
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + p.height + 0.5F + dy, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-escala * 0.8F, -escala * 0.8F, escala * 0.8F);  // un poco más chica que el nombre
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int j = f.getStringWidth(s) / 2;
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        GlStateManager.disableTexture2D();
        w.begin(7, DefaultVertexFormats.POSITION_COLOR);
        w.pos(-j - 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        w.pos(-j - 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        w.pos(j + 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        w.pos(j + 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        t.draw();
        GlStateManager.enableTexture2D();
        f.drawString(s, -j, 0, 0x20FFFFFF);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        f.drawString(s, -j, 0, 0xFFFFFFFF);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
