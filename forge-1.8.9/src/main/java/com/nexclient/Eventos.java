package com.nexclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Eventos {
    private final Minecraft mc = Minecraft.getMinecraft();

    /** Mira personalizada en lugar de la del juego; en tercera persona y con la vista libre no hay mira. */
    @SubscribeEvent
    public void mira(RenderGameOverlayEvent.Pre e) {
        if (e.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        Config c = Config.get();
        boolean tercera = mc.gameSettings.thirdPersonView != 0 || VistaLibre.activa;
        if (tercera && !(c.mira && c.miraTercera)) {
            e.setCanceled(true);
            return;
        }
        if (c.mira && !mc.gameSettings.showDebugInfo && mc.thePlayer != null) {
            e.setCanceled(true);
            if (c.miraOcultarArco && mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse() != null
                    && mc.thePlayer.getItemInUse().getItem() instanceof net.minecraft.item.ItemBow) {
                return;
            }
            Hud.mira(e.resolution.getScaledWidth() / 2, e.resolution.getScaledHeight() / 2);
        }
    }

    @SubscribeEvent
    public void hud(RenderGameOverlayEvent.Post e) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL) {
            Hud.dibujar(false);
        }
    }

    private static final int ID_BOTON = 98170;

    /** Menú principal y de pausa de NexClient en lugar de los de Minecraft (o del paquete de texturas). */
    /** Servidor al que NexLauncher pidió entrar (Amigos, «Entrar directo»); se usa una sola vez. */
    private static String entrarA = System.getProperty("nexclient.servidor");

    @SubscribeEvent
    public void abrirMenu(net.minecraftforge.client.event.GuiOpenEvent e) {
        if (entrarA != null) {
            if (e.gui instanceof net.minecraft.client.multiplayer.GuiConnecting) {
                entrarA = null;  // Minecraft ya está entrando con --server
            } else if (e.gui instanceof net.minecraft.client.gui.GuiMainMenu) {
                String direccion = entrarA.trim();
                entrarA = null;
                if (!direccion.isEmpty()) {
                    net.minecraft.client.gui.GuiScreen menu = Config.get().menuNex ? new MenuPrincipalNex() : e.gui;
                    e.gui = new net.minecraft.client.multiplayer.GuiConnecting(menu, mc,
                            new net.minecraft.client.multiplayer.ServerData("NexLauncher", direccion, false));
                    return;
                }
            }
        }
        if (!Config.get().menuNex) {
            return;
        }
        if (e.gui instanceof net.minecraft.client.gui.GuiMainMenu) {
            e.gui = new MenuPrincipalNex();
        } else if (e.gui instanceof GuiIngameMenu) {
            e.gui = new MenuPausaNex();
        }
    }

    /** Botón «NexClient» en el menú de pausa (Esc): centrado, justo encima de «Volver al juego». */
    @SubscribeEvent
    public void menuPausa(GuiScreenEvent.InitGuiEvent.Post e) {
        if (!(e.gui instanceof GuiIngameMenu)) {
            return;
        }
        GuiButton volver = null;
        for (GuiButton b : e.buttonList) {
            if (b.id == 4) {  // «Volver al juego»
                volver = b;
            }
        }
        if (volver == null) {
            for (GuiButton b : e.buttonList) {
                if (volver == null || b.yPosition < volver.yPosition) {
                    volver = b;
                }
            }
        }
        if (volver == null) {
            return;
        }
        int y = volver.yPosition;
        for (GuiButton b : e.buttonList) {
            if (b.yPosition >= y) {
                b.yPosition += 24;  // todo baja una fila para hacerle lugar
            }
        }
        e.buttonList.add(new GuiButton(ID_BOTON, volver.xPosition, y, volver.width, 20, "NexClient"));
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
        if (e.dwheel != 0 && Movimiento.zoomActivo && mc.currentScreen == null) {
            Movimiento.rueda(e.dwheel);  // con el zoom puesto la rueda cambia el aumento, no la ranura
            e.setCanceled(true);
            return;
        }
        if (!e.buttonstate || e.button < 0 || mc.currentScreen != null) {
            return;
        }
        Cps.clic(e.button);
        if (e.button == 0) {
            Efectos.clicIzquierdo();
        }
    }

    /** Etiqueta de NexLauncher debajo del nombre (después de que el juego dibuja el nombre). */
    @SubscribeEvent
    public void etiqueta(net.minecraftforge.client.event.RenderLivingEvent.Specials.Post<net.minecraft.entity.EntityLivingBase> e) {
        if (e.entity instanceof net.minecraft.client.entity.AbstractClientPlayer) {
            Etiquetas.dibujar((net.minecraft.client.entity.AbstractClientPlayer) e.entity, e.x, e.y, e.z);
        }
    }

    @SubscribeEvent
    public void ataque(AttackEntityEvent e) {
        if (e.entityPlayer != null && e.entityPlayer.worldObj.isRemote && e.entityPlayer == mc.thePlayer) {
            Efectos.golpe(e.entityPlayer, e.target);
        }
    }

    private int ticks;
    private boolean zoomRevisado;

    /**
     * OptiFine trae su propio zoom, también en la C: con los dos a la vez el aumento se multiplicaba y la
     * rueda no hacía nada. Si su tecla es la misma que la de NexClient, se le quita (una sola vez); el zoom
     * de NexClient hace lo mismo y además se ajusta con la rueda.
     */
    private void sinZoomOptiFine() {
        if (!Config.get().zoom || Movimiento.teclaZoom == null) {
            return;
        }
        for (KeyBinding k : mc.gameSettings.keyBindings) {
            if ("of.key.zoom".equals(k.getKeyDescription()) && k != Movimiento.teclaZoom
                    && k.getKeyCode() == Movimiento.teclaZoom.getKeyCode() && k.getKeyCode() != 0) {
                k.setKeyCode(0);
                KeyBinding.resetKeyBindingArrayAndHash();
                mc.gameSettings.saveOptions();
                System.out.println("[NexClient] Zoom de OptiFine desactivado: lo hace NexClient (tecla "
                        + org.lwjgl.input.Keyboard.getKeyName(Movimiento.teclaZoom.getKeyCode()) + ").");
            }
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            Movimiento.tick(mc);
            VistaLibre.tick(mc);
            if (!zoomRevisado && mc.gameSettings != null) {
                zoomRevisado = true;
                sinZoomOptiFine();
            }
        } else {
            Efectos.tick();
            Lan.tick(mc);
            Rastros.tick(mc);
            if (mc.thePlayer != null && mc.theWorld != null) {  // etiquetas y cosméticos de los jugadores cercanos
                java.util.List<String> cerca = new java.util.ArrayList<String>();
                for (net.minecraft.entity.player.EntityPlayer otro : mc.theWorld.playerEntities) {
                    if (otro != mc.thePlayer && otro.getDistanceSqToEntity(mc.thePlayer) < 96 * 96) {
                        cerca.add(otro.getName());
                    }
                }
                Cosmeticos.tick(mc.thePlayer.getName(),
                        mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "", cerca);
            }
            if (++ticks % 40 == 0 && mc.currentScreen == null) {
                Config.recargarSiCambio();
            }
        }
    }
}
