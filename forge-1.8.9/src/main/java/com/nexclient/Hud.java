package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Todo lo que NexClient dibuja en pantalla. Cada elemento se puede mover en el editor. */
public final class Hud {
    public static final String[] IDS = {"teclas", "cps", "fps", "coords", "ping", "reloj", "velocidad", "combo",
            "alcance", "estado", "pociones", "armadura"};
    public static final String[] NOMBRES = {"Teclas", "CPS", "FPS", "Coordenadas", "Ping", "Reloj", "Velocidad",
            "Combo", "Alcance", "Sprint / Sneak", "Pociones", "Armadura"};
    private static final String[] RUMBOS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
    private static final String[] ROMANOS = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
    private static final float[] BRILLO = new float[7];
    private static long ultimo = System.nanoTime();

    private Hud() {
    }

    public static boolean activo(Config c, String id) {
        if (id.equals("teclas")) return c.teclas;
        if (id.equals("cps")) return c.cps;
        if (id.equals("fps")) return c.fps;
        if (id.equals("coords")) return c.coords;
        if (id.equals("ping")) return c.ping;
        if (id.equals("reloj")) return c.reloj;
        if (id.equals("velocidad")) return c.velocidad;
        if (id.equals("combo")) return c.combo;
        if (id.equals("alcance")) return c.alcance;
        if (id.equals("estado")) return c.estado;
        if (id.equals("pociones")) return c.pociones;
        if (id.equals("armadura")) return c.armadura;
        return false;
    }

    public static String nombre(String id) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) {
                return NOMBRES[i];
            }
        }
        return id;
    }

    public static void dibujar(boolean editor) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!editor && (mc.gameSettings.hideGUI || mc.gameSettings.showDebugInfo || mc.thePlayer == null)) {
            return;
        }
        long ahora = System.nanoTime();
        float dt = Math.min(0.1f, (ahora - ultimo) / 1.0e9f);
        ultimo = ahora;
        Config c = Config.get();
        for (String id : IDS) {
            if (activo(c, id)) {
                dibujarUno(mc, c, id, editor, dt);
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Dibuja un elemento con su estilo (escala, fondo, borde, colores). */
    public static void dibujarUno(Minecraft mc, Config c, String id, boolean editor, float dt) {
        int[] r = rect(id, mc);
        Config.Estilo e = c.estilo(id);
        int[] b = base(id, mc, c);
        GlStateManager.pushMatrix();
        GlStateManager.translate(r[0], r[1], 0);
        GlStateManager.scale(e.escala, e.escala, 1f);
        if (id.equals("teclas")) {
            teclas(mc, c, e, dt);
        } else {
            if (e.fondo > 0) {
                Gui.drawRect(0, 0, b[0], b[1], Math.round(e.fondo * 2.55f) << 24);
            }
            if (e.borde) {
                contorno(0, 0, b[0], b[1], Config.COLORES[e.colorBorde]);
            }
            if (id.equals("armadura")) {
                armadura(mc, e, editor);
            } else {
                int y = 3;
                FontRenderer f = mc.fontRendererObj;
                for (String s : lineas(mc, c, id, editor)) {
                    f.drawString(s, (b[0] - f.getStringWidth(s)) / 2f, y, Config.COLORES[e.colorTexto], e.sombra);
                    y += 10;
                }
            }
        }
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void contorno(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y1 + 1, color);
        Gui.drawRect(x1, y2 - 1, x2, y2, color);
        Gui.drawRect(x1, y1, x1 + 1, y2, color);
        Gui.drawRect(x2 - 1, y1, x2, y2, color);
    }

    public static List<String> lineas(Minecraft mc, Config c, String id, boolean editor) {
        List<String> l = new ArrayList<String>();
        EntityPlayerSP p = mc.thePlayer;
        if (id.equals("cps")) {
            l.add(Cps.izquierdo() + " | " + Cps.derecho() + " CPS");
        } else if (id.equals("fps")) {
            l.add(Minecraft.getDebugFPS() + " FPS");
        } else if (id.equals("coords")) {
            if (p != null) {
                String s = "XYZ: " + MathHelper.floor_double(p.posX) + " " + MathHelper.floor_double(p.posY) + " "
                        + MathHelper.floor_double(p.posZ);
                if (c.coordsDireccion) {
                    int i = ((Math.round(p.rotationYaw / 45f) % 8) + 8) % 8;
                    s += "  " + RUMBOS[i];
                }
                l.add(s);
            } else if (editor) {
                l.add("XYZ: 0 64 0  N");
            }
        } else if (id.equals("ping")) {
            int ms = 0;
            if (p != null && mc.getNetHandler() != null) {
                NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(p.getUniqueID());
                ms = info == null ? 0 : info.getResponseTime();
            }
            l.add(ms + " ms");
        } else if (id.equals("reloj")) {
            Calendar t = Calendar.getInstance();
            int h = t.get(Calendar.HOUR_OF_DAY), m = t.get(Calendar.MINUTE);
            l.add(c.reloj24 ? String.format("%02d:%02d", h, m)
                    : String.format("%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h < 12 ? "a. m." : "p. m."));
        } else if (id.equals("velocidad")) {
            l.add(String.format("%.2f b/s", Efectos.velocidad));
        } else if (id.equals("combo")) {
            l.add(Efectos.combo() + " combo");
        } else if (id.equals("alcance")) {
            l.add(Efectos.alcance() < 0 ? "- alcance" : String.format("%.2f bloques", Efectos.alcance()));
        } else if (id.equals("estado")) {
            String s = Movimiento.textoEstado(mc);
            if (s != null) {
                l.add(s);
            } else if (editor) {
                l.add("[Sprint (fijo)]");
            }
        } else if (id.equals("pociones")) {
            if (p != null) {
                for (PotionEffect e : p.getActivePotionEffects()) {
                    Potion pot = Potion.potionTypes[e.getPotionID()];
                    if (pot == null) {
                        continue;
                    }
                    int a = Math.max(0, Math.min(ROMANOS.length - 1, e.getAmplifier()));
                    l.add(I18n.format(pot.getName()) + ROMANOS[a] + "  " + Potion.getDurationString(e));
                }
            }
            if (l.isEmpty() && editor) {
                l.add("Velocidad II  1:30");
            }
        }
        return l;
    }

    public static int altoTeclas(Config c) {
        int h = 46;
        if (c.teclasRaton) {
            h += 24;
        }
        if (c.teclasEspacio) {
            h += 12;
        }
        return h;
    }

    private static int[] posGuardada(Config c, String id, int indice) {
        if (id.equals("teclas")) {
            return new int[]{c.teclasX, c.teclasY};
        }
        if (id.equals("cps")) {
            return new int[]{c.cpsX, c.cpsY};
        }
        if (id.equals("fps")) {
            return new int[]{c.fpsX, c.fpsY};
        }
        int[] p = c.pos.get(id);
        if (p != null && p.length == 2) {
            return p;
        }
        if (id.equals("pociones")) {
            return new int[]{9999, 40};
        }
        if (id.equals("armadura")) {
            return new int[]{9999, 120};
        }
        return new int[]{6, 128 + (indice - 3) * 14};
    }

    /** Tamaño sin escalar del elemento. */
    public static int[] base(String id, Minecraft mc, Config c) {
        if (id.equals("teclas")) {
            return new int[]{70, altoTeclas(c)};
        }
        if (id.equals("armadura")) {
            return new int[]{56, 5 * 17};
        }
        List<String> l = lineas(mc, c, id, true);
        int w = 20;
        for (String s : l) {
            w = Math.max(w, mc.fontRendererObj.getStringWidth(s) + 8);
        }
        return new int[]{w, Math.max(1, l.size()) * 10 + 3};
    }

    public static int[] rect(String id, Minecraft mc) {
        Config c = Config.get();
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
        int indice = 0;
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) {
                indice = i;
            }
        }
        int[] p = posGuardada(c, id, indice);
        int[] b = base(id, mc, c);
        float e = c.estilo(id).escala;
        int w = Math.round(b[0] * e), h = Math.round(b[1] * e);
        int x = Math.max(0, Math.min(p[0], sw - w));
        int y = Math.max(0, Math.min(p[1], sh - h));
        return new int[]{x, y, w, h};
    }

    public static void restablecer(String id) {
        Config c = Config.get();
        c.estilos.remove(id);
        if (id.equals("teclas")) {
            c.teclasX = 6;
            c.teclasY = 6;
            c.teclasEscala = 1f;
        } else if (id.equals("cps")) {
            c.cpsX = 6;
            c.cpsY = 100;
        } else if (id.equals("fps")) {
            c.fpsX = 6;
            c.fpsY = 114;
        } else {
            c.pos.remove(id);
        }
    }

    public static void poner(Config c, String id, boolean v) {
        if (id.equals("teclas")) c.teclas = v;
        else if (id.equals("cps")) c.cps = v;
        else if (id.equals("fps")) c.fps = v;
        else if (id.equals("coords")) c.coords = v;
        else if (id.equals("ping")) c.ping = v;
        else if (id.equals("reloj")) c.reloj = v;
        else if (id.equals("velocidad")) c.velocidad = v;
        else if (id.equals("combo")) c.combo = v;
        else if (id.equals("alcance")) c.alcance = v;
        else if (id.equals("estado")) c.estado = v;
        else if (id.equals("pociones")) c.pociones = v;
        else if (id.equals("armadura")) c.armadura = v;
    }

    public static void mover(String id, int x, int y) {
        Config c = Config.get();
        if (id.equals("teclas")) {
            c.teclasX = x;
            c.teclasY = y;
        } else if (id.equals("cps")) {
            c.cpsX = x;
            c.cpsY = y;
        } else if (id.equals("fps")) {
            c.fpsX = x;
            c.fpsY = y;
        } else {
            c.pos.put(id, new int[]{x, y});
        }
    }

    private static void armadura(Minecraft mc, Config.Estilo e, boolean editor) {
        EntityPlayerSP p = mc.thePlayer;
        ItemStack[] items = new ItemStack[5];
        if (p != null) {
            for (int i = 0; i < 4; i++) {
                items[i] = p.inventory.armorInventory[3 - i];
            }
            items[4] = p.getHeldItem();
        }
        int y = 0;
        for (ItemStack s : items) {
            if (s != null) {
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(s, 1, y);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                String t = s.isItemStackDamageable() ? String.valueOf(s.getMaxDamage() - s.getItemDamage())
                        : (s.stackSize > 1 ? String.valueOf(s.stackSize) : "");
                mc.fontRendererObj.drawString(t, 20, y + 4, Config.COLORES[e.colorTexto], e.sombra);
                GlStateManager.enableDepth();
            } else if (editor) {
                mc.fontRendererObj.drawString("-", 20, y + 4, 0xFF888888);
            }
            y += 17;
        }
    }

    private static void teclas(Minecraft mc, Config c, Config.Estilo e, float dt) {
        GameSettings o = mc.gameSettings;
        KeyBinding[] k = {o.keyBindForward, o.keyBindLeft, o.keyBindBack, o.keyBindRight, o.keyBindAttack,
                o.keyBindUseItem, o.keyBindJump};
        for (int i = 0; i < k.length; i++) {
            float objetivo = k[i].isKeyDown() ? 1f : 0f;
            float vel = objetivo > BRILLO[i] ? 28f : 9f;
            BRILLO[i] += (objetivo - BRILLO[i]) * Math.min(1f, dt * vel);
        }
        FontRenderer f = mc.fontRendererObj;
        int fondo = Math.round(e.fondo * 2.55f) << 24;
        int texto = Config.COLORES[e.colorTexto];
        int borde = e.borde ? Config.COLORES[e.colorBorde] : 0;
        tecla(f, 24, 0, 22, 22, nombre(k[0]), null, BRILLO[0], fondo, texto, borde, e.sombra);
        tecla(f, 0, 24, 22, 22, nombre(k[1]), null, BRILLO[1], fondo, texto, borde, e.sombra);
        tecla(f, 24, 24, 22, 22, nombre(k[2]), null, BRILLO[2], fondo, texto, borde, e.sombra);
        tecla(f, 48, 24, 22, 22, nombre(k[3]), null, BRILLO[3], fondo, texto, borde, e.sombra);
        int y = 48;
        if (c.teclasRaton) {
            tecla(f, 0, y, 34, 22, "LMB", c.teclasCps ? Cps.izquierdo() + " CPS" : null, BRILLO[4], fondo, texto, borde, e.sombra);
            tecla(f, 36, y, 34, 22, "RMB", c.teclasCps ? Cps.derecho() + " CPS" : null, BRILLO[5], fondo, texto, borde, e.sombra);
            y += 24;
        }
        if (c.teclasEspacio) {
            Gui.drawRect(0, y, 70, y + 10, mezclar(fondo, 0xCCFFFFFF, BRILLO[6]));
            if (borde != 0) {
                contorno(0, y, 70, y + 10, borde);
            }
            Gui.drawRect(22, y + 4, 48, y + 6, mezclar(texto, 0xFF000000, BRILLO[6]));
        }
    }

    private static String nombre(KeyBinding k) {
        String s = GameSettings.getKeyDisplayString(k.getKeyCode());
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    private static void tecla(FontRenderer f, int x, int y, int w, int h, String texto, String sub, float b,
                              int fondo, int colorTexto, int borde, boolean sombraBase) {
        Gui.drawRect(x, y, x + w, y + h, mezclar(fondo, 0xCCFFFFFF, b));
        if (borde != 0) {
            contorno(x, y, x + w, y + h, borde);
        }
        int color = mezclar(colorTexto, 0xFF000000, b);
        boolean sombra = sombraBase && b < 0.5f;
        if (sub == null) {
            f.drawString(texto, x + (w - f.getStringWidth(texto)) / 2f, y + (h - 8) / 2f, color, sombra);
        } else {
            f.drawString(texto, x + (w - f.getStringWidth(texto)) / 2f, y + 3, color, sombra);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + w / 2f, y + 13, 0);
            GlStateManager.scale(0.6f, 0.6f, 1f);
            f.drawString(sub, -f.getStringWidth(sub) / 2f, 0, color, sombra);
            GlStateManager.popMatrix();
        }
    }

    public static int mezclar(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }
}
