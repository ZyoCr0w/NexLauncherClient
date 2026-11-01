package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.common.ProgressManager;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Pantalla de carga de NexClient en lugar de la de Mojang y la de Forge: el logo con «NexClient · Cargando…» y una
 * sola barra, sin detalles. Se redibuja en cada paso de la carga (en el hilo del juego, como la original).
 */
public final class PantallaCarga {
    private static int textura = -1;
    private static long ultima;
    private static float mostrado;
    public static volatile boolean terminada;

    private PantallaCarga() {
    }

    /** La imagen se lee del .jar a mano: al principio de la carga el juego aún no conoce los recursos de los mods. */
    private static int textura() {
        if (textura == -1) {
            textura = 0;
            try {
                InputStream in = PantallaCarga.class.getResourceAsStream("/assets/nexclient/textures/gui/cargando.png");
                if (in != null) {
                    BufferedImage img = ImageIO.read(in);
                    in.close();
                    textura = TextureUtil.glGenTextures();
                    TextureUtil.uploadTextureImageAllocate(textura, img, true, true);
                }
            } catch (Exception e) {
                textura = 0;
            }
        }
        return textura;
    }

    /** Qué tanto lleva la carga (0..1), con lo que avisa Forge en sus barras (la de afuera y la que está dentro). */
    private static float progreso() {
        float p = 0, escala = 1;
        try {
            Iterator<ProgressManager.ProgressBar> it = ProgressManager.barIterator();
            while (it.hasNext()) {
                ProgressManager.ProgressBar b = it.next();
                int pasos = Math.max(1, b.getSteps());
                p += escala * Math.max(0, b.getStep() - (it.hasNext() ? 1 : 0)) / pasos;
                escala /= pasos;
            }
        } catch (Exception e) {
            // sin barras todavía
        }
        mostrado = Math.max(mostrado, Math.min(1, p));
        return mostrado;
    }

    /** Desde Forge en cada paso: como mucho 30 veces por segundo. */
    public static void refrescar() {
        Minecraft mc = Minecraft.getMinecraft();
        long ahora = System.currentTimeMillis();
        if (terminada || ahora - ultima < 33 || mc == null || !mc.isCallingFromMinecraftThread()) {
            return;
        }
        ultima = ahora;
        dibujar();
    }

    public static void dibujar() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth(), h = sr.getScaledHeight();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, w, h, 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0, 0, -2000);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.disableAlpha();
        boolean dm = Temporada.muertos();
        if (dm) {
            GlStateManager.clearColor(0.09F, 0.035F, 0.13F, 1F);
        } else {
            GlStateManager.clearColor(0.043F, 0.043F, 0.063F, 1F);
        }
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int iw = Math.min(w - 40, 300), ih = iw / 4;
        int x = (w - iw) / 2, y = h / 2 - ih / 2 - 8;
        int t = textura();
        Tessellator te = Tessellator.getInstance();
        WorldRenderer wr = te.getWorldRenderer();
        if (t > 0) {
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(t);
            GlStateManager.color(1F, 1F, 1F, 1F);
            wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            wr.pos(x, y + ih, 0).tex(0, 1).endVertex();
            wr.pos(x + iw, y + ih, 0).tex(1, 1).endVertex();
            wr.pos(x + iw, y, 0).tex(1, 0).endVertex();
            wr.pos(x, y, 0).tex(0, 0).endVertex();
            te.draw();
        }
        GlStateManager.disableTexture2D();
        int by = y + ih + 16, bw = iw - 60, bx = (w - bw) / 2;
        rect(wr, te, bx, by, bw, 3, 1F, 1F, 1F, 0.16F);
        if (dm) {  // barra de cempasúchil y papel picado debajo
            rect(wr, te, bx, by, Math.round(bw * progreso()), 3, 1F, 0.56F, 0.12F, 1F);
            for (int i = 0, n = Math.max(1, bw / 14); i < n; i++) {
                int c = Temporada.PAPEL[i % Temporada.PAPEL.length];
                rect(wr, te, bx + i * 14 + 2, by + 12, 11, 8, ((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F, 0.85F);
            }
        } else {
            rect(wr, te, bx, by, Math.round(bw * progreso()), 3, 1F, 1F, 1F, 1F);
        }
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        mc.updateDisplay();
    }

    private static void rect(WorldRenderer wr, Tessellator te, int x, int y, int w, int h, float r, float g, float b, float a) {
        if (w <= 0) {
            return;
        }
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x, y + h, 0).color(r, g, b, a).endVertex();
        wr.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
        wr.pos(x + w, y, 0).color(r, g, b, a).endVertex();
        wr.pos(x, y, 0).color(r, g, b, a).endVertex();
        te.draw();
    }
}
