package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.HitResult;

/** Blockhit (cuándo se bloquea, su sonido) y multiplicador de partículas de golpe. */
public final class Efectos {
    private static final String[] IDS = {"block.anvil.land", "item.shield.block", "entity.player.attack.strong",
            "entity.experience_orb.pickup", "block.note_block.pling", "ui.button.click"};
    private static long ultimoBloqueo;
    /** Bloques por segundo (horizontal), golpes seguidos sin recibir daño y distancia del último golpe. */
    public static double velocidad;
    private static int combo;
    private static long ultimoGolpe;
    private static double alcance = -1;
    private static boolean herido;
    private static LivingEntity objetivo;
    private static int colorAplicado = Integer.MIN_VALUE;

    private Efectos() {
    }

    /**
     * En 1.21 la espada ya no bloquea: se considera «bloqueando» al tener una espada en la mano y mantener
     * el clic derecho (o al levantar un escudo de la otra mano), como hacen los clientes PvP.
     */
    public static boolean bloqueando(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || !(p.getMainHandItem().getItem() instanceof SwordItem)) {
            return false;
        }
        if (p.isUsingItem()) {  // comiendo con la otra mano no cuenta; levantando escudo sí
            return p.getUseItem().getItem() instanceof ShieldItem;
        }
        return mc.options.keyUse.isDown();
    }

    /** Comiendo o bebiendo (comida, pociones, leche…). */
    public static boolean comiendo(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || !p.isUsingItem()) {
            return false;
        }
        UseAnim a = p.getUseItem().getUseAnimation();
        return a == UseAnim.EAT || a == UseAnim.DRINK;
    }

    /** Clic izquierdo mientras comes: el juego no golpea, pero en 1.7 el brazo sí se movía (sin sonido). */
    public static void clicIzquierdo(Minecraft mc) {
        Config c = Config.get();
        LocalPlayer p = mc.player;
        if (p != null && c.blockhitComida && comiendo(mc)) {
            ((LivingEntity) p).swing(p.getUsedItemHand(), false);  // solo la animación, sin paquete
        }
    }

    public static void tick(Minecraft mc) {
        if (bloqueando(mc)) {
            ultimoBloqueo = System.currentTimeMillis();
        }
        aplicarColorGolpe(mc);
        LocalPlayer p = mc.player;
        if (p == null) {
            combo = 0;
            return;
        }
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        velocidad = velocidad * 0.5 + Math.sqrt(dx * dx + dz * dz) * 20 * 0.5;
        boolean ahoraHerido = p.hurtTime > 0;
        if (ahoraHerido && !herido) {
            combo = 0;  // te pegaron: se corta el combo
        }
        herido = ahoraHerido;
    }

    /** Último ser vivo que golpeaste (para el panel «Objetivo»), durante 4 s. */
    public static LivingEntity objetivo() {
        if (objetivo == null || objetivo.isRemoved() || !objetivo.isAlive()
                || System.currentTimeMillis() - ultimoGolpe > 4000) {
            return null;
        }
        return objetivo;
    }

    /** Color del parpadeo al recibir daño: se repinta la textura de «overlay» del juego. */
    private static void aplicarColorGolpe(Minecraft mc) {
        Config c = Config.get();
        int deseado = c.colorGolpe ? Config.COLORES[c.colorGolpeIdx] : -1;
        if (deseado == colorAplicado || mc.gameRenderer == null) {
            return;
        }
        try {
            net.minecraft.client.renderer.texture.DynamicTexture tex =
                    ((com.nexclient.mixin.OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).nexclient$textura();
            com.mojang.blaze3d.platform.NativeImage img = tex.getPixels();
            if (img == null) {
                return;
            }
            int abgr = deseado == -1 ? -1308622593  // el rojo original de Minecraft
                    : (0xB2 << 24) | ((deseado & 255) << 16) | (((deseado >> 8) & 255) << 8) | ((deseado >> 16) & 255);
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 16; x++) {
                    img.setPixelRGBA(x, y, abgr);
                }
            }
            tex.upload();
            colorAplicado = deseado;
        } catch (Throwable e) {
            colorAplicado = deseado;  // no reintentar cada tick
            System.err.println("[NexClient] Color de golpe no disponible: " + e);
        }
    }

    public static int combo() {
        return System.currentTimeMillis() - ultimoGolpe > 2500 ? 0 : combo;
    }

    public static double alcance() {
        return System.currentTimeMillis() - ultimoGolpe > 4000 ? -1 : alcance;
    }

    public static void golpe(Player jugador, Entity objetivo) {
        Minecraft mc = Minecraft.getInstance();
        Config c = Config.get();
        if (System.currentTimeMillis() - ultimoGolpe > 2500) {
            combo = 0;
        }
        combo++;
        ultimoGolpe = System.currentTimeMillis();
        if (objetivo instanceof LivingEntity) {
            Efectos.objetivo = (LivingEntity) objetivo;
        }
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            alcance = jugador.getEyePosition().distanceTo(mc.hitResult.getLocation());
        } else {
            alcance = Math.sqrt(objetivo.getBoundingBox().distanceToSqr(jugador.getEyePosition()));
        }
        boolean blockhit = bloqueando(mc) || System.currentTimeMillis() - ultimoBloqueo < 350;
        if (c.sonido && (c.sonidoCadaGolpe || blockhit)) {
            sonar();
        }
        if (!c.particulas || mc.particleEngine == null) {
            return;
        }
        int n = Math.max(1, c.multiplicador);
        boolean critico = jugador.fallDistance > 0 && !jugador.onGround() && !jugador.onClimbable()
                && !jugador.isInWater() && !jugador.isPassenger();
        if (critico || c.siempreCritico) {
            // el juego ya dibuja una tanda en los críticos: se agregan las que faltan
            for (int i = critico ? 1 : 0; i < n; i++) {
                mc.particleEngine.createTrackingEmitter(objetivo, ParticleTypes.CRIT);
            }
        }
        ItemStack arma = jugador.getMainHandItem();
        if (arma.isEnchanted() || c.siempreFilo) {
            for (int i = arma.isEnchanted() ? 1 : 0; i < n; i++) {
                mc.particleEngine.createTrackingEmitter(objetivo, ParticleTypes.ENCHANTED_HIT);
            }
        }
    }

    public static void sonar() {
        Config c = Config.get();
        int i = Math.max(0, Config.indiceSonido(c.sonidoId));
        SoundEvent ev = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace(IDS[i]));
        if (ev != null && c.volumen > 0) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, c.tono, c.volumen));
        }
    }
}
