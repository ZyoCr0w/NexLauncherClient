package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentText;

/**
 * Mundo abierto en LAN: Minecraft solo deja entrar cuentas premium. Con «lanSinPremium» (lo pone
 * NexLauncher en la pestaña Amigos) también entran amigos con cuenta no premium.
 */
public final class Lan {
    private static boolean avisado;

    private Lan() {
    }

    public static void tick(Minecraft mc) {
        IntegratedServer s = mc.getIntegratedServer();
        if (s == null || !s.getPublic()) {
            avisado = false;
            return;
        }
        if (Config.get().lanSinPremium && s.isServerInOnlineMode()) {
            s.setOnlineMode(false);
            if (!avisado && mc.thePlayer != null) {
                avisado = true;
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        "\u00a77[NexClient] Tu mundo LAN acepta cuentas no premium."));
            }
        }
    }
}
