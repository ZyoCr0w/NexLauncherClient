package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;

/** Blockhit (animación y sonido) y multiplicador de partículas de golpe. */
public final class Efectos {
    private static final String[] IDS = {"random.anvil_land", "mob.irongolem.hit", "random.successful_hit",
            "random.orb", "note.pling", "random.click"};
    private static long ultimoBloqueo;

    private Efectos() {
    }

    public static boolean bloqueando() {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        return p != null && p.isBlocking();
    }

    public static void tick() {
        if (bloqueando()) {
            ultimoBloqueo = System.currentTimeMillis();
        }
    }

    /** Clic izquierdo mientras bloqueas: el juego no golpea, pero en 1.7 la espada sí se movía. */
    public static void clicIzquierdo() {
        Config c = Config.get();
        if (!bloqueando()) {
            return;
        }
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (c.blockhit && c.estilo17 && (!p.isSwingInProgress || p.swingProgressInt >= 3 || p.swingProgressInt < 0)) {
            p.swingProgressInt = -1;  // solo la animación: no se manda nada al servidor
            p.isSwingInProgress = true;
        }
        if (c.sonido) {
            sonar();
        }
    }

    public static void golpe(EntityPlayer jugador, Entity objetivo) {
        Minecraft mc = Minecraft.getMinecraft();
        Config c = Config.get();
        boolean blockhit = bloqueando() || System.currentTimeMillis() - ultimoBloqueo < 350;
        if (c.sonido && (c.sonidoCadaGolpe || blockhit)) {
            sonar();
        }
        if (!c.particulas || mc.effectRenderer == null) {
            return;
        }
        int n = Math.max(1, c.multiplicador);
        boolean critico = jugador.fallDistance > 0 && !jugador.onGround && !jugador.isOnLadder()
                && !jugador.isInWater() && !jugador.isPotionActive(Potion.blindness) && jugador.ridingEntity == null;
        if (critico || c.siempreCritico) {
            for (int i = critico ? 1 : 0; i < n; i++) {
                mc.effectRenderer.emitParticleAtEntity(objetivo, EnumParticleTypes.CRIT);
            }
        }
        boolean filo = objetivo instanceof EntityLivingBase && EnchantmentHelper.getModifierForCreature(
                jugador.getHeldItem(), ((EntityLivingBase) objetivo).getCreatureAttribute()) > 0;
        if (filo || c.siempreFilo) {
            for (int i = filo ? 1 : 0; i < n; i++) {
                mc.effectRenderer.emitParticleAtEntity(objetivo, EnumParticleTypes.CRIT_MAGIC);
            }
        }
    }

    public static void sonar() {
        Minecraft mc = Minecraft.getMinecraft();
        Config c = Config.get();
        if (mc.thePlayer == null || c.volumen <= 0) {
            return;
        }
        int i = Math.max(0, Config.indiceSonido(c.sonidoId));
        mc.getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(IDS[i]), c.volumen, c.tono,
                (float) mc.thePlayer.posX, (float) mc.thePlayer.posY, (float) mc.thePlayer.posZ));
    }
}
