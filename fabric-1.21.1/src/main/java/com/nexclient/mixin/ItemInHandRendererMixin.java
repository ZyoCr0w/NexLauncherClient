package com.nexclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nexclient.Config;
import com.nexclient.Efectos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blockhit visual: con espada y clic derecho, la espada se dibuja en la pose de bloqueo de 1.8;
 * con «estilo 1.7» además sigue el golpe mientras bloqueas.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    private void applyItemArmTransform(PoseStack pose, HumanoidArm brazo, float equipado) {
        throw new AssertionError();
    }

    @Shadow
    private void applyItemArmAttackTransform(PoseStack pose, HumanoidArm brazo, float golpe) {
        throw new AssertionError();
    }

    @Shadow
    public abstract void renderItem(LivingEntity entidad, ItemStack item, ItemDisplayContext contexto, boolean izquierda,
                                    PoseStack pose, MultiBufferSource buffer, int luz);

    /** Comer como el juego, pero además con el golpe del brazo (animación de 1.7). */
    private void comerGolpeando(AbstractClientPlayer jugador, float parcial, InteractionHand mano, float golpe,
                                ItemStack item, float equipado, PoseStack pose, MultiBufferSource buffer, int luz) {
        HumanoidArm brazo = mano == InteractionHand.MAIN_HAND ? jugador.getMainArm() : jugador.getMainArm().getOpposite();
        boolean derecha = brazo == HumanoidArm.RIGHT;
        int s = derecha ? 1 : -1;
        pose.pushPose();
        float f = (float) jugador.getUseItemRemainingTicks() - parcial + 1.0f;
        float g = f / (float) item.getUseDuration(jugador);
        if (g < 0.8f) {
            pose.translate(0.0f, Math.abs((float) Math.cos(f / 4.0f * (float) Math.PI) * 0.1f), 0.0f);
        }
        float h = 1.0f - (float) Math.pow(g, 27.0);
        pose.translate(h * 0.6f * s, h * -0.5f, 0.0f);
        pose.mulPose(Axis.YP.rotationDegrees(s * h * 90.0f));
        pose.mulPose(Axis.XP.rotationDegrees(h * 10.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(s * h * 30.0f));
        applyItemArmTransform(pose, brazo, equipado);
        applyItemArmAttackTransform(pose, brazo, golpe);
        renderItem(jugador, item, derecha ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !derecha, pose, buffer, luz);
        pose.popPose();
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void nexclient$blockhit(AbstractClientPlayer jugador, float parcial, float pitch, InteractionHand mano,
                                    float golpe, ItemStack item, float equipado, PoseStack pose,
                                    MultiBufferSource buffer, int luz, CallbackInfo ci) {
        Config c = Config.get();
        Minecraft mc = Minecraft.getInstance();
        if (c.blockhitComida && Efectos.comiendo(mc) && jugador.getUsedItemHand() == mano
                && jugador.getUseItem() == item) {
            comerGolpeando(jugador, parcial, mano, golpe, item, equipado, pose, buffer, luz);
            ci.cancel();
            return;
        }
        if (!c.blockhit || !Efectos.bloqueando(mc)) {
            return;
        }
        if (mano == InteractionHand.OFF_HAND) {
            if (c.ocultarEscudo && item.getItem() instanceof ShieldItem) {
                ci.cancel();
            }
            return;
        }
        if (!(item.getItem() instanceof SwordItem)) {
            return;
        }
        HumanoidArm brazo = jugador.getMainArm();
        boolean derecha = brazo == HumanoidArm.RIGHT;
        float s = derecha ? 1f : -1f;
        pose.pushPose();
        applyItemArmTransform(pose, brazo, equipado);
        if (c.estilo17) {
            applyItemArmAttackTransform(pose, brazo, golpe);
        }
        // Pose de bloqueo de la espada de 1.8, llevada al sistema de coordenadas actual
        pose.translate(s * -0.14142136f, 0.08f, 0.14142136f);
        pose.mulPose(Axis.XP.rotationDegrees(-102.25f));
        pose.mulPose(Axis.YP.rotationDegrees(s * 13.365f));
        pose.mulPose(Axis.ZP.rotationDegrees(s * 78.05f));
        renderItem(jugador, item, derecha ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !derecha, pose, buffer, luz);
        pose.popPose();
        ci.cancel();
    }
}
