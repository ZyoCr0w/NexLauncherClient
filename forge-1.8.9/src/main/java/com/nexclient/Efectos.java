package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.EnumAction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

/** Blockhit (animación y sonido) y multiplicador de partículas de golpe. */
public final class Efectos {
    private static final String[] IDS = {"random.anvil_land", "mob.irongolem.hit", "random.successful_hit",
            "random.orb", "note.pling", "random.click"};
    private static long ultimoBloqueo;
    public static double velocidad;
    private static int combo;
    private static long ultimoGolpe;
    private static double alcance = -1;
    private static boolean herido;
    private static EntityLivingBase objetivo;

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
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p == null) {
            combo = 0;
            return;
        }
        double dx = p.posX - p.prevPosX, dz = p.posZ - p.prevPosZ;
        velocidad = velocidad * 0.5 + Math.sqrt(dx * dx + dz * dz) * 20 * 0.5;
        boolean ahoraHerido = p.hurtTime > 0;
        if (ahoraHerido && !herido) {
            combo = 0;
        }
        herido = ahoraHerido;
    }

    /** Último ser vivo que golpeaste (para el panel «Objetivo»), durante 4 s. */
    public static EntityLivingBase objetivo() {
        if (objetivo == null || objetivo.isDead || objetivo.getHealth() <= 0
                || System.currentTimeMillis() - ultimoGolpe > 4000) {
            return null;
        }
        return objetivo;
    }

    public static int combo() {
        return System.currentTimeMillis() - ultimoGolpe > 2500 ? 0 : combo;
    }

    public static double alcance() {
        return System.currentTimeMillis() - ultimoGolpe > 4000 ? -1 : alcance;
    }

    /** Clic izquierdo mientras bloqueas: el juego no golpea, pero en 1.7 la espada sí se movía. */
    /** Comiendo o bebiendo (comida, pociones, leche…). */
    public static boolean comiendo() {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p == null || !p.isUsingItem() || p.getItemInUse() == null) {
            return false;
        }
        EnumAction a = p.getItemInUse().getItemUseAction();
        return a == EnumAction.EAT || a == EnumAction.DRINK;
    }

    /** Clic izquierdo mientras bloqueas o comes: el juego no golpea, pero en 1.7 el brazo sí se movía. */
    public static void clicIzquierdo() {
        Config c = Config.get();
        boolean espada = bloqueando();
        boolean comida = !espada && c.blockhitComida && comiendo();
        if (!espada && !comida) {
            return;
        }
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        boolean animar = comida || (c.blockhit && c.estilo17);
        if (animar && (!p.isSwingInProgress || p.swingProgressInt >= 3 || p.swingProgressInt < 0)) {
            p.swingProgressInt = -1;  // solo la animación: no se manda nada al servidor
            p.isSwingInProgress = true;
        }
        if (espada && c.sonido) {  // el sonido de blockhit es solo con la espada
            sonar();
        }
    }

    public static void golpe(EntityPlayer jugador, Entity objetivo) {
        Minecraft mc = Minecraft.getMinecraft();
        Config c = Config.get();
        if (System.currentTimeMillis() - ultimoGolpe > 2500) {
            combo = 0;
        }
        combo++;
        ultimoGolpe = System.currentTimeMillis();
        if (objetivo instanceof EntityLivingBase) {
            Efectos.objetivo = (EntityLivingBase) objetivo;
        }
        Vec3 ojos = jugador.getPositionEyes(1.0F);
        if (mc.objectMouseOver != null && mc.objectMouseOver.hitVec != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            alcance = ojos.distanceTo(mc.objectMouseOver.hitVec);
        } else {
            alcance = ojos.distanceTo(new Vec3(objetivo.posX, objetivo.posY + objetivo.height / 2, objetivo.posZ));
        }
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
