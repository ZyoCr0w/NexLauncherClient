package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

/**
 * Mundo abierto en LAN: Minecraft solo deja entrar cuentas premium. Con «lanSinPremium» (lo pone
 * NexLauncher en la pestaña Amigos) también entran amigos con cuenta no premium.
 */
public final class Lan {
    private static boolean avisado;

    private Lan() {
    }

    public static void tick(Minecraft mc) {
        IntegratedServer s = mc.getSingleplayerServer();
        if (s == null || !s.isPublished()) {
            avisado = false;
            return;
        }
        if (Config.get().lanSinPremium && s.usesAuthentication()) {
            s.setUsesAuthentication(false);
            if (!avisado && mc.player != null) {
                avisado = true;
                mc.player.displayClientMessage(
                        Component.literal("\u00a77[NexClient] Tu mundo LAN acepta cuentas no premium."), false);
            }
        }
    }
}
