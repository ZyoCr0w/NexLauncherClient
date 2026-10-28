package com.nexclient.mixin;

import com.nexclient.Config;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hora fija: solo cambia lo que ves tú (el servidor sigue con su hora). */
@Mixin(ClientLevel.ClientLevelData.class)
public class ClientLevelDataMixin {
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true, require = 0)
    private void nexclient$hora(CallbackInfoReturnable<Long> cir) {
        Config c = Config.get();
        if (c.tiempoFijo) {
            cir.setReturnValue((long) c.hora);
        }
    }
}
