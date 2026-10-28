package com.nexclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Arrastra cada elemento del HUD a donde quieras. */
public class EditorHudScreen extends Screen {
    private final Screen padre;
    private String arrastrando;
    private double dx, dy;

    public EditorHudScreen(Screen padre) {
        super(Component.literal("Mover HUD"));
        this.padre = padre;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x44000000);  // sin desenfoque: se ve el juego detrás
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        Hud.dibujar(g, true);
        Config c = Config.get();
        int activos = 0;
        for (String id : Hud.IDS) {
            if (!Hud.activo(c, id)) {
                continue;
            }
            activos++;
            int[] r = Hud.rect(id, minecraft);
            boolean encima = id.equals(arrastrando) || dentro(r, mx, my);
            g.renderOutline(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, encima ? 0xFFFFFFFF : 0x88FFFFFF);
            if (encima) {
                g.drawString(font, Hud.nombre(id), r[0], Math.max(0, r[1] - 10), 0xFFFFFFFF, true);
            }
        }
        g.drawCenteredString(font, activos == 0 ? "Activa elementos en el menú de NexClient (pestañas HUD e Info)"
                : "Arrastra los elementos · ESC para terminar", width / 2, height - 20, 0xFFFFFF);
    }

    private static boolean dentro(int[] r, double x, double y) {
        return x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3];
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        if (boton == 0) {
            Config c = Config.get();
            for (int i = Hud.IDS.length - 1; i >= 0; i--) {  // el de encima primero
                String id = Hud.IDS[i];
                int[] r = Hud.rect(id, minecraft);
                if (Hud.activo(c, id) && dentro(r, mx, my)) {
                    arrastrando = id;
                    dx = mx - r[0];
                    dy = my - r[1];
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, boton);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int boton, double ddx, double ddy) {
        if (arrastrando != null) {
            Hud.mover(arrastrando, (int) Math.round(mx - dx), (int) Math.round(my - dy));
            int[] r = Hud.rect(arrastrando, minecraft);  // ya ajustado a la pantalla
            Hud.mover(arrastrando, r[0], r[1]);
            return true;
        }
        return super.mouseDragged(mx, my, boton, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int boton) {
        arrastrando = null;
        return super.mouseReleased(mx, my, boton);
    }

    @Override
    public void onClose() {
        Config.get().guardar();
        minecraft.setScreen(padre);
    }
}
