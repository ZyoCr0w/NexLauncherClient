package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;

/** Rastros al caminar: chispas, corazones, notas o pisadas de fuego (partículas del juego). */
public final class Rastros {
    private static int tick;

    private Rastros() {
    }

    public static void tick(Minecraft mc) {
        if (mc.theWorld == null || mc.thePlayer == null || mc.isGamePaused() || ++tick % 2 != 0) {
            return;
        }
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            Cosmeticos.Jugador j = Cosmeticos.de(p.getName());
            String id = j == null ? null : j.de("rastro");
            if (id == null || p.isInvisible() || !p.onGround || p.getDistanceSqToEntity(mc.thePlayer) > 48 * 48
                    || !Cosmeticos.mostrar(p == mc.thePlayer)) {
                continue;
            }
            double dx = p.posX - p.prevPosX, dz = p.posZ - p.prevPosZ;
            if (dx * dx + dz * dz < 0.0025) {
                continue;
            }
            EnumParticleTypes tipo;
            double vy = 0.02;
            if (id.equals("chispas")) {
                tipo = EnumParticleTypes.FIREWORKS_SPARK;
                vy = 0.05;
            } else if (id.equals("corazones")) {
                tipo = EnumParticleTypes.HEART;
            } else if (id.equals("notas")) {
                tipo = EnumParticleTypes.NOTE;
            } else if (id.equals("pisadas_fuego")) {
                tipo = EnumParticleTypes.FLAME;
                vy = 0.01;
            } else {
                continue;
            }
            double ox = (p.getRNG().nextDouble() - 0.5) * 0.5, oz = (p.getRNG().nextDouble() - 0.5) * 0.5;
            double vx = id.equals("notas") ? p.getRNG().nextInt(25) / 24.0 : 0;
            mc.theWorld.spawnParticle(tipo, p.posX + ox, p.posY + (id.equals("notas") || id.equals("corazones") ? 0.4 : 0.05),
                    p.posZ + oz, vx, id.equals("chispas") ? 0.02 : vy, 0);
        }
    }
}
