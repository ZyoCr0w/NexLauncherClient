package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Color de golpe: cambia el rojo (1, 0, 0) del parpadeo al recibir daño. */
@Mixin(RendererLivingEntity.class)
public class MixinRendererLivingEntity {
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
}
