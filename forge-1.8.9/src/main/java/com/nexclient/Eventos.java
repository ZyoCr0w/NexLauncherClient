package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Eventos {
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void hud(RenderGameOverlayEvent.Post e) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL) {
            Hud.dibujar(false);
        }
    }

    @SubscribeEvent
    public void teclas(InputEvent.KeyInputEvent e) {
        if (NexClient.TECLA_MENU.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new MenuNex(null));
        }
    }

    @SubscribeEvent
    public void raton(MouseEvent e) {
        if (!e.buttonstate || e.button < 0 || mc.currentScreen != null) {
            return;
        }
        Cps.clic(e.button);
        if (e.button == 0) {
            Efectos.clicIzquierdo();
        }
    }

    @SubscribeEvent
    public void ataque(AttackEntityEvent e) {
        if (e.entityPlayer != null && e.entityPlayer.worldObj.isRemote && e.entityPlayer == mc.thePlayer) {
            Efectos.golpe(e.entityPlayer, e.target);
        }
    }

    private int ticks;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            Movimiento.tick(mc);
        } else {
            Efectos.tick();
            if (++ticks % 40 == 0 && mc.currentScreen == null) {
                Config.recargarSiCambio();
            }
        }
    }
}
