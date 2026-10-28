package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Siempre despejado: sin lluvia ni tormenta en tu pantalla. */
@Mixin(Level.class)
public class LevelMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$lluvia(float parcial, CallbackInfoReturnable<Float> cir) {
        if (Config.get().despejado && (Object) this instanceof ClientLevel) {
            cir.setReturnValue(0f);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$tormenta(float parcial, CallbackInfoReturnable<Float> cir) {
        if (Config.get().despejado && (Object) this instanceof ClientLevel) {
            cir.setReturnValue(0f);
        }
    }
}
