package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

/** Rastros al caminar: chispas, corazones, notas o pisadas de fuego (partículas del juego). */
public final class Rastros {
    private static int tick;

    private Rastros() {
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused() || ++tick % 2 != 0) {
            return;
        }
        for (Player p : mc.level.players()) {
            Cosmeticos.Jugador j = Cosmeticos.de(p.getGameProfile().getName());
            String id = j == null ? null : j.de("rastro");
            if (id == null || p.isInvisible() || p.isSpectator() || !p.onGround() || p.distanceToSqr(mc.player) > 48 * 48
                    || !Cosmeticos.mostrar(p == mc.player)) {
                continue;
            }
            double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
            if (dx * dx + dz * dz < 0.0025) {  // quieto: sin rastro
                continue;
            }
            ParticleOptions tipo;
            double vy = 0.02;
            switch (id) {
                case "chispas": tipo = ParticleTypes.ELECTRIC_SPARK; vy = 0.08; break;
                case "corazones": tipo = ParticleTypes.HEART; break;
                case "notas": tipo = ParticleTypes.NOTE; break;
                case "pisadas_fuego": tipo = tick % 4 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME; vy = 0.01; break;
                default: continue;
            }
            double ox = (p.getRandom().nextDouble() - 0.5) * 0.5, oz = (p.getRandom().nextDouble() - 0.5) * 0.5;
            // en NOTE la velocidad x elige el color de la nota
            double vx = id.equals("notas") ? p.getRandom().nextInt(25) / 24.0 : 0;
            mc.level.addParticle(tipo, p.getX() + ox, p.getY() + (id.equals("notas") || id.equals("corazones") ? 0.4 : 0.05),
                    p.getZ() + oz, vx, vy, 0);
        }
    }
}
