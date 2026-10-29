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
            if (activo(c, id)) {
                dibujarUno(g, mc, c, id, editor, dt);
            }
        }
    }

    /** Dibuja un elemento con su estilo (escala, fondo, borde, colores). */
    public static void dibujarUno(GuiGraphics g, Minecraft mc, Config c, String id, boolean editor, float dt) {
        int[] r = rect(id, mc);
        Config.Estilo e = c.estilo(id);
        int[] b = base(id, mc, c);
        g.pose().pushPose();
        g.pose().translate(r[0], r[1], 0);
        g.pose().scale(e.escala, e.escala, 1f);
        if (id.equals("teclas")) {
            teclas(g, mc, c, e, dt);
        } else {
            if (e.fondo > 0) {
                g.fill(0, 0, b[0], b[1], (Math.round(e.fondo * 2.55f) << 24));
            }
            if (e.borde) {
                g.renderOutline(0, 0, b[0], b[1], Config.COLORES[e.colorBorde]);
            }
            if (id.equals("armadura")) {
                armadura(g, mc, e, editor);
            } else {
                int y = 3;
                for (String s : lineas(mc, c, id, editor)) {
                    texto(g, mc.font, s, (b[0] - mc.font.width(s)) / 2, y, Config.COLORES[e.colorTexto], e.sombra,
                            e.arcoiris);
                    y += 10;
                }
            }
        }
        g.pose().popPose();
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

    /** Tamaño sin escalar del elemento. */
    public static int[] base(String id, Minecraft mc, Config c) {
        if (id.equals("teclas")) {
            return new int[]{70, altoTeclas(c)};
        }
        if (id.equals("armadura")) {
            return new int[]{56, ARMADURA.length * 17};
        }
        List<String> l = lineas(mc, c, id, true);
        int w = 20;
        for (String s : l) {
            w = Math.max(w, mc.font.width(s) + 8);
        }
        return new int[]{w, Math.max(1, l.size()) * 10 + 3};
    }

    /** {x, y, ancho, alto} del elemento ya escalado, sin salirse de la pantalla. */
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

    public static boolean poner(Config c, String id, boolean v) {
        switch (id) {
            case "teclas": c.teclas = v; break;
            case "cps": c.cps = v; break;
            case "fps": c.fps = v; break;
            case "coords": c.coords = v; break;
            case "ping": c.ping = v; break;
            case "reloj": c.reloj = v; break;
            case "velocidad": c.velocidad = v; break;
            case "combo": c.combo = v; break;
            case "alcance": c.alcance = v; break;
            case "estado": c.estado = v; break;
            case "pociones": c.pociones = v; break;
            case "armadura": c.armadura = v; break;
            default: return false;
        }
        return true;
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

    // ---- dibujo (coordenadas locales: el elemento ya está trasladado y escalado)
    private static void armadura(GuiGraphics g, Minecraft mc, Config.Estilo e, boolean editor) {
        LocalPlayer p = mc.player;
        int y = 0;
        for (EquipmentSlot slot : ARMADURA) {
            ItemStack s = p == null ? ItemStack.EMPTY : p.getItemBySlot(slot);
            if (!s.isEmpty()) {
                g.renderItem(s, 1, y);
                String t = s.isDamageableItem() ? String.valueOf(s.getMaxDamage() - s.getDamageValue())
                        : (s.getCount() > 1 ? String.valueOf(s.getCount()) : "");
                texto(g, mc.font, t, 20, y + 4, Config.COLORES[e.colorTexto], e.sombra, e.arcoiris);
            } else if (editor) {
                g.drawString(mc.font, "—", 20, y + 4, 0xFF888888, false);
            }
            y += 17;
        }
    }

    private static void teclas(GuiGraphics g, Minecraft mc, Config c, Config.Estilo e, float dt) {
        KeyMapping[] k = {mc.options.keyUp, mc.options.keyLeft, mc.options.keyDown, mc.options.keyRight,
                mc.options.keyAttack, mc.options.keyUse, mc.options.keyJump};
        for (int i = 0; i < k.length; i++) {
            float objetivo = k[i].isDown() ? 1f : 0f;
            // se ilumina rápido y se apaga suave, igual a cualquier FPS
            float vel = objetivo > BRILLO[i] ? 28f : 9f;
            BRILLO[i] += (objetivo - BRILLO[i]) * Math.min(1f, dt * vel);
        }
        Font f = mc.font;
        int fondo = Math.round(e.fondo * 2.55f) << 24;
        int texto = Config.COLORES[e.colorTexto];
        int borde = e.borde ? Config.COLORES[e.colorBorde] : 0;
        arcoTeclas = e.arcoiris;
        tecla(g, f, 24, 0, 22, 22, nombre(k[0]), null, BRILLO[0], fondo, texto, borde, e.sombra);
        tecla(g, f, 0, 24, 22, 22, nombre(k[1]), null, BRILLO[1], fondo, texto, borde, e.sombra);
        tecla(g, f, 24, 24, 22, 22, nombre(k[2]), null, BRILLO[2], fondo, texto, borde, e.sombra);
        tecla(g, f, 48, 24, 22, 22, nombre(k[3]), null, BRILLO[3], fondo, texto, borde, e.sombra);
        int y = 48;
        if (c.teclasRaton) {
            tecla(g, f, 0, y, 34, 22, "LMB", c.teclasCps ? Cps.izquierdo() + " CPS" : null, BRILLO[4], fondo, texto,
                    borde, e.sombra);
            tecla(g, f, 36, y, 34, 22, "RMB", c.teclasCps ? Cps.derecho() + " CPS" : null, BRILLO[5], fondo, texto,
                    borde, e.sombra);
            y += 24;
        }
        if (c.teclasEspacio) {
            g.fill(0, y, 70, y + 10, mezclar(fondo, 0xCCFFFFFF, BRILLO[6]));
            if (borde != 0) {
                g.renderOutline(0, y, 70, 10, borde);
            }
            g.fill(22, y + 4, 48, y + 6, mezclar(texto, 0xFF000000, BRILLO[6]));
        }
    }

    private static String nombre(KeyMapping k) {
        String s = k.getTranslatedKeyMessage().getString();
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    /** Texto normal, o arcoíris: el tono avanza con el tiempo y con la posición de cada letra. */
    public static void texto(GuiGraphics g, Font f, String s, int x, int y, int color, boolean sombra, boolean arcoiris) {
        if (!arcoiris) {
            g.drawString(f, s, x, y, color, sombra);
            return;
        }
        float t = (System.currentTimeMillis() % 4000L) / 4000f;
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            float tono = (t + (cx - x) / 90f) % 1f;
            g.drawString(f, ch, cx, y, 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(1f - tono, 0.8f, 1f), sombra);
            cx += f.width(ch);
        }
    }

    private static boolean arcoTeclas;

    private static void tecla(GuiGraphics g, Font f, int x, int y, int w, int h, String texto, String sub, float b,
                              int fondo, int colorTexto, int borde, boolean sombraBase) {
        g.fill(x, y, x + w, y + h, mezclar(fondo, 0xCCFFFFFF, b));
        if (borde != 0) {
            g.renderOutline(x, y, w, h, borde);
        }
        int color = mezclar(colorTexto, 0xFF000000, b);
        boolean sombra = sombraBase && b < 0.5f;
        if (sub == null) {
            texto(g, f, texto, x + (w - f.width(texto)) / 2, y + (h - 8) / 2, color, sombra, arcoTeclas && b < 0.5f);
        } else {
            texto(g, f, texto, x + (w - f.width(texto)) / 2, y + 3, color, sombra, arcoTeclas && b < 0.5f);
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, y + 13, 0);
            g.pose().scale(0.6f, 0.6f, 1f);
            texto(g, f, sub, -f.width(sub) / 2, 0, color, sombra, arcoTeclas && b < 0.5f);
            g.pose().popPose();
        }
    }

    /** Mira personalizada, centrada en (cx, cy). */
    public static void mira(GuiGraphics g, int cx, int cy) {
        Config c = Config.get();
        int s = c.miraTamano, t = c.miraGrosor, h = c.miraHueco, color = Config.COLORES[c.miraColor];
        int a = t / 2;
        java.util.List<int[]> r = new java.util.ArrayList<int[]>();
        int estilo = c.miraEstilo;
        if (estilo == 0 || estilo == 1 || estilo == 4) {
            r.add(new int[]{cx - h - s, cy - a, cx - h, cy - a + t});          // izquierda
            r.add(new int[]{cx + h + 1, cy - a, cx + h + 1 + s, cy - a + t});  // derecha
            r.add(new int[]{cx - a, cy + h + 1, cx - a + t, cy + h + 1 + s});  // abajo
            if (estilo != 4) {
                r.add(new int[]{cx - a, cy - h - s, cx - a + t, cy - h});      // arriba
            }
        }
        if (estilo == 1 || estilo == 2) {
            int d = Math.max(1, t);
            r.add(new int[]{cx - d / 2, cy - d / 2, cx - d / 2 + d, cy - d / 2 + d});
        }
        if (estilo == 3) {
            int radio = s + h;
            for (int i = 0; i < 32; i++) {
                double ang = Math.PI * 2 * i / 32;
                int x = cx + (int) Math.round(Math.cos(ang) * radio), y = cy + (int) Math.round(Math.sin(ang) * radio);
                r.add(new int[]{x - a, y - a, x - a + t, y - a + t});
            }
        }
        if (c.miraBorde) {
            for (int[] q : r) {
                g.fill(q[0] - 1, q[1] - 1, q[2] + 1, q[3] + 1, 0xC0000000);
            }
        }
        for (int[] q : r) {
            g.fill(q[0], q[1], q[2], q[3], color);
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
