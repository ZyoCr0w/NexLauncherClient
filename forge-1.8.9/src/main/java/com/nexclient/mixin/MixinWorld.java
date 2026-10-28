package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hora fija y siempre despejado: solo en tu pantalla (el servidor no cambia). */
@Mixin(World.class)
public abstract class MixinWorld {
    @Shadow
    @Final
    public WorldProvider provider;

    @Inject(method = "getCelestialAngle", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$hora(float parcial, CallbackInfoReturnable<Float> cir) {
        Config c = Config.get();
        if (c.tiempoFijo && (Object) this instanceof WorldClient) {
            cir.setReturnValue(provider.calculateCelestialAngle(c.hora, parcial));
        }
    }

    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$lluvia(float parcial, CallbackInfoReturnable<Float> cir) {
        if (Config.get().despejado && (Object) this instanceof WorldClient) {
            cir.setReturnValue(0f);
        }
    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$tormenta(float parcial, CallbackInfoReturnable<Float> cir) {
        if (Config.get().despejado && (Object) this instanceof WorldClient) {
            cir.setReturnValue(0f);
        }
    }
}
