package com.nexclient.mixin;

import com.nexclient.Config;
import com.nexclient.Cosmeticos;
import com.nexclient.Etiquetas;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Color de golpe, tu propio nombre en tercera persona y el nombre más arriba si llevas algo en la cabeza. */
@Mixin(RendererLivingEntity.class)
public class MixinRendererLivingEntity {
    private static boolean nexclient$subiendo;

    private static float nexclient$canal(int desplazamiento, float original) {
        Config c = Config.get();
        if (!c.colorGolpe) {
            return original;
        }
        return ((Config.COLORES[c.colorGolpeIdx] >> desplazamiento) & 255) / 255f;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE",
            target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0), require = 0)
    private float nexclient$rojo(float v) {
        return nexclient$canal(16, v);
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE",
            target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1), require = 0)
    private float nexclient$verde(float v) {
        return nexclient$canal(8, v);
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE",
            target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2), require = 0)
    private float nexclient$azul(float v) {
        return nexclient$canal(0, v);
    }

    /** En F5 el juego no dibuja tu nombre; con la opción activada, sí. */
    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$nombrePropio(EntityLivingBase e, CallbackInfoReturnable<Boolean> cir) {
        if (Etiquetas.nombrePropio(e)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    /** Sube el nombre (y la etiqueta, que va con él) si el jugador lleva orejas, corona, aureola… */
    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$subirNombre(EntityLivingBase e, double x, double y, double z, CallbackInfo ci) {
        if (nexclient$subiendo || !(e instanceof EntityPlayer)) {
            return;
        }
        float s = Cosmeticos.subidaNombre(e.getName(), e == net.minecraft.client.Minecraft.getMinecraft().thePlayer);
        if (s <= 0) {
            return;
        }
        ci.cancel();
        nexclient$subiendo = true;
        try {
            ((RendererLivingEntity) (Object) this).renderName(e, x, y + s, z);
        } finally {
            nexclient$subiendo = false;
        }
    }
}
