package com.nexclient;

import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Autotexto: cuando el chat anuncia el final de una partida («Winner:», «1st Killer -», «Ganador:»…) se manda solo
 * el mensaje elegido (por ejemplo «gg»), un segundo después y una sola vez por partida.
 */
public final class AutoTexto {
    private static long ultimo;
    private static int espera = -1;

    private AutoTexto() {
    }

    /** Cada línea del chat que llega del servidor (sin formato). */
    public static void mensaje(String texto) {
        Config c = Config.get();
        if (!c.autoTexto || texto == null || c.autoTextoMensaje == null || c.autoTextoMensaje.trim().isEmpty()) {
            return;
        }
        if (espera >= 0 || System.currentTimeMillis() - ultimo < 15000) {
            return;  // ya va a mandarse, o se mandó hace poco (los finales traen varias líneas)
        }
        if (esFinal(texto, c.autoTextoFrases)) {
            espera = 20;  // un segundo: algunos servidores ignoran lo que se escribe justo al terminar
        }
    }

    /** ¿La línea anuncia el final de una partida? Lo que escriben los jugadores no cuenta. */
    static boolean esFinal(String texto, List<String> frases) {
        String t = texto.replaceAll("§.", "");
        if (t.startsWith("<") || frases == null) {
            return false;
        }
        int dosPuntos = t.indexOf(": ");
        for (String f : frases) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            int i = t.indexOf(f);
            // «[VIP] Steve: Winner: yo» lo escribió un jugador: la frase está después de su nombre
            if (i >= 0 && (dosPuntos < 0 || dosPuntos >= i)) {
                return true;
            }
        }
        return false;
    }

    public static void tick(Minecraft mc) {
        if (espera < 0) {
            return;
        }
        if (mc.thePlayer == null) {
            espera = -1;
            return;
        }
        if (--espera <= 0) {
            espera = -1;
            ultimo = System.currentTimeMillis();
            mc.thePlayer.sendChatMessage(Config.get().autoTextoMensaje.trim());  // con / al inicio va como comando
        }
    }
}
