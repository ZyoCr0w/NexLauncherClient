package com.nexclient.mixin;

import com.nexclient.PantallaCarga;
import net.minecraftforge.fml.common.ProgressManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cada paso de la carga de Forge mueve la barra de la pantalla de carga de NexClient. */
@Mixin(value = ProgressManager.ProgressBar.class, remap = false)
public abstract class MixinBarraCarga {
    @Inject(method = "step(Ljava/lang/String;)V", at = @At("TAIL"), require = 0)
    private void nexclient$paso(String mensaje, CallbackInfo ci) {
        PantallaCarga.refrescar();
    }
}
