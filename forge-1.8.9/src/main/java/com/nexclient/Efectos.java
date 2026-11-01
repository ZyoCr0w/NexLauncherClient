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
    /**
     * Sonidos propios de NexClient (en su espacio «nexclient»): un paquete de texturas con sonidos cambia los de
     * Minecraft, pero no estos, así que el blockhit siempre suena como el del cliente.
     */
    private static final String[] IDS = {"blockhit.yunque", "blockhit.golpe", "blockhit.ding", "blockhit.orbe",
            "blockhit.pling", "blockhit.clic"};
    private static long ultimoBloqueo;
    public static double velocidad;
    private static int combo;
    private static long ultimoGolpe;
    private static double alcance = -1;
    private static boolean herido;
    private static int hurtAntes;

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
        mantenerGolpe();
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
        // te acaban de pegar (el juego reinicia hurtTime en 10): suena el blockhit si bloqueabas con la espada
        if (p.hurtTime > hurtAntes && Config.get().cuandoSuena() == 0 && Config.get().sonido && bloqueoReciente(250)) {
            sonar();
        }
        hurtAntes = p.hurtTime;
    }

    /** Bloqueando ahora o hace muy poco (el aviso del daño llega del servidor un instante después). */
    private static boolean bloqueoReciente(long ms) {
        return bloqueando() || System.currentTimeMillis() - ultimoBloqueo < ms;
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

    /**
     * Clic izquierdo mantenido mientras bloqueas o comes: el brazo repite el golpe como si spamearas clics
     * (se reinicia a la mitad de la animación, como el swing del juego). Solo visual, sin paquetes ni sonido.
     */
    private static void mantenerGolpe() {
        Minecraft mc = Minecraft.getMinecraft();
        Config c = Config.get();
        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.currentScreen != null || !c.blockhitMantener || !mc.gameSettings.keyBindAttack.isKeyDown()) {
            return;
        }
        boolean espada = c.blockhit && c.estilo17 && bloqueando();
        boolean comida = c.blockhitComida && comiendo();
        if ((espada || comida) && (!p.isSwingInProgress || p.swingProgressInt >= 3 || p.swingProgressInt < 0)) {
            p.swingProgressInt = -1;
            p.isSwingInProgress = true;
        }
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
        if (espada && c.sonido && c.cuandoSuena() == 1) {  // el sonido de blockhit es solo con la espada
            sonar();
        }
    }

    public static void golpe(EntityPlayer jugador, Entity objetivo) {
        Miras.ultimoGolpe = System.currentTimeMillis();
        Minecraft mc = Minecraft.getMinecraft();
        Config c = Config.get();
        if (System.currentTimeMillis() - ultimoGolpe > 2500) {
            combo = 0;
        }
        combo++;
        ultimoGolpe = System.currentTimeMillis();
        Vec3 ojos = jugador.getPositionEyes(1.0F);
        if (mc.objectMouseOver != null && mc.objectMouseOver.hitVec != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            alcance = ojos.distanceTo(mc.objectMouseOver.hitVec);
        } else {
            alcance = ojos.distanceTo(new Vec3(objetivo.posX, objetivo.posY + objetivo.height / 2, objetivo.posZ));
        }
        if (c.sonido && (c.cuandoSuena() == 2 || (c.cuandoSuena() == 1 && bloqueoReciente(350)))) {
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
        mc.getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation("nexclient", IDS[i]), c.volumen, c.tono,
                (float) mc.thePlayer.posX, (float) mc.thePlayer.posY, (float) mc.thePlayer.posZ));
    }
}
