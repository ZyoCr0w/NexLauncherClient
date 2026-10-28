package com.nexclient;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Todo lo que NexClient dibuja en pantalla. Cada elemento se puede mover en el editor. */
public final class Hud {
    public static final String[] IDS = {"teclas", "cps", "fps", "coords", "ping", "reloj", "velocidad", "combo",
            "alcance", "estado", "pociones", "armadura"};
    public static final String[] NOMBRES = {"Teclas", "CPS", "FPS", "Coordenadas", "Ping", "Reloj", "Velocidad",
            "Combo", "Alcance", "Sprint / Sneak", "Pociones", "Armadura"};
    private static final String[] RUMBOS = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
    private static final String[] ROMANOS = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
    private static final EquipmentSlot[] ARMADURA = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    private static final float[] BRILLO = new float[7];
    private static long ultimo = System.nanoTime();

    private Hud() {
    }

    public static boolean activo(Config c, String id) {
        switch (id) {
            case "teclas": return c.teclas;
            case "cps": return c.cps;
            case "fps": return c.fps;
            case "coords": return c.coords;
            case "ping": return c.ping;
            case "reloj": return c.reloj;
            case "velocidad": return c.velocidad;
            case "combo": return c.combo;
            case "alcance": return c.alcance;
            case "estado": return c.estado;
            case "pociones": return c.pociones;
            case "armadura": return c.armadura;
            default: return false;
        }
    }

    public static String nombre(String id) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) {
                return NOMBRES[i];
            }
        }
        return id;
    }

    public static void dibujar(GuiGraphics g, boolean editor) {
        Minecraft mc = Minecraft.getInstance();
        if (!editor && (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || mc.player == null)) {
            return;
        }
        long ahora = System.nanoTime();
        float dt = Math.min(0.1f, (ahora - ultimo) / 1.0e9f);
        ultimo = ahora;
        Config c = Config.get();
        for (String id : IDS) {
            if (!activo(c, id)) {
                continue;
            }
            if (id.equals("teclas")) {
                teclas(g, mc, c, dt);
            } else if (id.equals("armadura")) {
                armadura(g, mc, c, editor);
            } else {
                List<String> lineas = lineas(mc, c, id, editor);
                if (!lineas.isEmpty()) {
                    texto(g, mc.font, c, rect(id, mc), lineas);
                }
            }
        }
    }

    // ---- contenido de cada elemento de texto
    public static List<String> lineas(Minecraft mc, Config c, String id, boolean editor) {
        List<String> l = new ArrayList<String>();
        LocalPlayer p = mc.player;
        switch (id) {
            case "cps":
                l.add(Cps.izquierdo() + " | " + Cps.derecho() + " CPS");
                break;
            case "fps":
                l.add(mc.getFps() + " FPS");
                break;
            case "coords":
                if (p != null) {
                    String s = "XYZ: " + (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " "
                            + (int) Math.floor(p.getZ());
                    if (c.coordsDireccion) {
                        s += "  " + RUMBOS[Math.floorMod(Math.round(p.getYRot() / 45f), 8)];
                    }
                    l.add(s);
                } else if (editor) {
                    l.add("XYZ: 0 64 0  N");
                }
                break;
            case "ping": {
                int ms = 0;
                if (p != null && mc.getConnection() != null) {
                    PlayerInfo info = mc.getConnection().getPlayerInfo(p.getUUID());
                    ms = info == null ? 0 : info.getLatency();
                }
                l.add(ms + " ms");
                break;
            }
            case "reloj": {
                LocalTime t = LocalTime.now();
                int h = t.getHour();
                l.add(c.reloj24 ? String.format("%02d:%02d", h, t.getMinute())
                        : String.format("%d:%02d %s", h % 12 == 0 ? 12 : h % 12, t.getMinute(), h < 12 ? "a. m." : "p. m."));
                break;
            }
            case "velocidad":
                l.add(String.format("%.2f b/s", Efectos.velocidad));
                break;
            case "combo":
                l.add(Efectos.combo() + " combo");
                break;
            case "alcance":
                l.add(Efectos.alcance() < 0 ? "- alcance" : String.format("%.2f bloques", Efectos.alcance()));
                break;
            case "estado": {
                String s = Movimiento.textoEstado(mc);
                if (s != null) {
                    l.add(s);
                } else if (editor) {
                    l.add("[Sprint (fijo)]");
                }
                break;
            }
            case "pociones":
                if (p != null) {
                    for (MobEffectInstance e : p.getActiveEffects()) {
                        String t = e.isInfiniteDuration() ? "∞" : String.format("%d:%02d", e.getDuration() / 1200,
                                (e.getDuration() / 20) % 60);
                        int a = Math.max(0, Math.min(ROMANOS.length - 1, e.getAmplifier()));
                        l.add(e.getEffect().value().getDisplayName().getString() + ROMANOS[a] + "  " + t);
                    }
                }
                if (l.isEmpty() && editor) {
                    l.add("Velocidad II  1:30");
                }
                break;
            default:
                break;
        }
        return l;
    }

    // ---- posiciones (en coordenadas de la interfaz)
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

    /** {x, y, ancho, alto} del elemento, sin salirse de la pantalla. */
    public static int[] rect(String id, Minecraft mc) {
        Config c = Config.get();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int indice = 0;
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) {
                indice = i;
            }
        }
        int[] p = posGuardada(c, id, indice);
        int w, h;
        if (id.equals("teclas")) {
            w = Math.round(70 * c.teclasEscala);
            h = Math.round(altoTeclas(c) * c.teclasEscala);
        } else if (id.equals("armadura")) {
            w = 56;
            h = ARMADURA.length * 17;
        } else {
            List<String> l = lineas(mc, c, id, true);
            w = 20;
            for (String s : l) {
                w = Math.max(w, mc.font.width(s) + 8);
            }
            h = Math.max(1, l.size()) * 10 + 3;
        }
        int x = Math.max(0, Math.min(p[0], sw - w));
        int y = Math.max(0, Math.min(p[1], sh - h));
        return new int[]{x, y, w, h};
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

    // ---- dibujo
    private static void texto(GuiGraphics g, Font f, Config c, int[] r, List<String> lineas) {
        if (c.hudFondo) {
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x66000000);
        }
        int y = r[1] + 3;
        for (String s : lineas) {
            g.drawString(f, s, r[0] + (r[2] - f.width(s)) / 2, y, c.colorTexto(), c.hudSombra);
            y += 10;
        }
    }

    private static void armadura(GuiGraphics g, Minecraft mc, Config c, boolean editor) {
        int[] r = rect("armadura", mc);
        if (c.hudFondo) {
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x66000000);
        }
        LocalPlayer p = mc.player;
        int y = r[1];
        for (EquipmentSlot slot : ARMADURA) {
            ItemStack s = p == null ? ItemStack.EMPTY : p.getItemBySlot(slot);
            if (!s.isEmpty()) {
                g.renderItem(s, r[0] + 1, y);
                String t = s.isDamageableItem() ? String.valueOf(s.getMaxDamage() - s.getDamageValue())
                        : (s.getCount() > 1 ? String.valueOf(s.getCount()) : "");
                g.drawString(mc.font, t, r[0] + 20, y + 4, c.colorTexto(), c.hudSombra);
            } else if (editor) {
                g.drawString(mc.font, "—", r[0] + 20, y + 4, 0xFF888888, false);
            }
            y += 17;
        }
    }

    private static void teclas(GuiGraphics g, Minecraft mc, Config c, float dt) {
        int[] r = rect("teclas", mc);
        KeyMapping[] k = {mc.options.keyUp, mc.options.keyLeft, mc.options.keyDown, mc.options.keyRight,
                mc.options.keyAttack, mc.options.keyUse, mc.options.keyJump};
        for (int i = 0; i < k.length; i++) {
            float objetivo = k[i].isDown() ? 1f : 0f;
            // se ilumina rápido y se apaga suave, igual a cualquier FPS
            float vel = objetivo > BRILLO[i] ? 28f : 9f;
            BRILLO[i] += (objetivo - BRILLO[i]) * Math.min(1f, dt * vel);
        }
        g.pose().pushPose();
        g.pose().translate(r[0], r[1], 0);
        g.pose().scale(c.teclasEscala, c.teclasEscala, 1f);
        Font f = mc.font;
        tecla(g, f, 24, 0, 22, 22, nombre(k[0]), null, BRILLO[0]);
        tecla(g, f, 0, 24, 22, 22, nombre(k[1]), null, BRILLO[1]);
        tecla(g, f, 24, 24, 22, 22, nombre(k[2]), null, BRILLO[2]);
        tecla(g, f, 48, 24, 22, 22, nombre(k[3]), null, BRILLO[3]);
        int y = 48;
        if (c.teclasRaton) {
            tecla(g, f, 0, y, 34, 22, "LMB", c.teclasCps ? Cps.izquierdo() + " CPS" : null, BRILLO[4]);
            tecla(g, f, 36, y, 34, 22, "RMB", c.teclasCps ? Cps.derecho() + " CPS" : null, BRILLO[5]);
            y += 24;
        }
        if (c.teclasEspacio) {
            g.fill(0, y, 70, y + 10, mezclar(0x66000000, 0xCCFFFFFF, BRILLO[6]));
            g.fill(22, y + 4, 48, y + 6, mezclar(0xFFFFFFFF, 0xFF000000, BRILLO[6]));
        }
        g.pose().popPose();
    }

    private static String nombre(KeyMapping k) {
        String s = k.getTranslatedKeyMessage().getString();
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    private static void tecla(GuiGraphics g, Font f, int x, int y, int w, int h, String texto, String sub, float b) {
        g.fill(x, y, x + w, y + h, mezclar(0x66000000, 0xCCFFFFFF, b));
        int color = mezclar(0xFFFFFFFF, 0xFF000000, b);
        boolean sombra = b < 0.5f;
        if (sub == null) {
            g.drawString(f, texto, x + (w - f.width(texto)) / 2, y + (h - 8) / 2, color, sombra);
        } else {
            g.drawString(f, texto, x + (w - f.width(texto)) / 2, y + 3, color, sombra);
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, y + 13, 0);
            g.pose().scale(0.6f, 0.6f, 1f);
            g.drawString(f, sub, -f.width(sub) / 2, 0, color, sombra);
            g.pose().popPose();
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
