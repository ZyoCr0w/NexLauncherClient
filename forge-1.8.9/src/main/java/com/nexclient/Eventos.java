package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
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

    private static final int ID_BOTON = 98170;

    /** Botón «NexClient» en el menú de pausa (Esc), abajo a la izquierda. */
    @SubscribeEvent
    public void menuPausa(GuiScreenEvent.InitGuiEvent.Post e) {
        if (e.gui instanceof GuiIngameMenu) {
            e.buttonList.add(new GuiButton(ID_BOTON, 6, e.gui.height - 26, 98, 20, "NexClient"));
        }
    }

    @SubscribeEvent
    public void botonPausa(GuiScreenEvent.ActionPerformedEvent.Pre e) {
        if (e.gui instanceof GuiIngameMenu && e.button.id == ID_BOTON) {
            mc.displayGuiScreen(new MenuNex(e.gui));
            e.setCanceled(true);
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
