package com.nexclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Arrastra las teclas, los CPS y los FPS a donde quieras. */
public class EditorHudScreen extends Screen {
    private final Screen padre;
    private int arrastrando = -1;
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
        boolean[] activo = {c.teclas, c.cps, c.fps};
        for (int i = 0; i < 3; i++) {
            int[] r = Hud.rect(i, minecraft);
            boolean encima = i == arrastrando || dentro(r, mx, my);
            int borde = encima ? 0xFFFFFFFF : (activo[i] ? 0x99FFFFFF : 0x55FF5555);
            g.renderOutline(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, borde);
            if (!activo[i]) {
                g.drawString(font, Hud.NOMBRES[i] + " (apagado)", r[0], r[1] + r[3] + 2, 0xFFFF7777, true);
            }
        }
        g.drawCenteredString(font, "Arrastra los elementos · ESC para terminar", width / 2, height - 20, 0xFFFFFF);
    }

    private static boolean dentro(int[] r, double x, double y) {
        return x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3];
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        if (boton == 0) {
            for (int i = 0; i < 3; i++) {
                int[] r = Hud.rect(i, minecraft);
                if (dentro(r, mx, my)) {
                    arrastrando = i;
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
        if (arrastrando >= 0) {
            Hud.mover(arrastrando, (int) Math.round(mx - dx), (int) Math.round(my - dy));
            int[] r = Hud.rect(arrastrando, minecraft);  // ya ajustado a la pantalla
            Hud.mover(arrastrando, r[0], r[1]);
            return true;
        }
        return super.mouseDragged(mx, my, boton, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int boton) {
        arrastrando = -1;
        return super.mouseReleased(mx, my, boton);
    }

    @Override
    public void onClose() {
        Config.get().guardar();
        minecraft.setScreen(padre);
    }
}
