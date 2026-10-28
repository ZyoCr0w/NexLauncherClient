package com.nexclient;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Arrastra las teclas, los CPS y los FPS a donde quieras. */
public class EditorHud extends GuiScreen {
    private final GuiScreen padre;
    private int arrastrando = -1;
    private int dx, dy;

    public EditorHud(GuiScreen padre) {
        this.padre = padre;
    }

    @Override
    public void drawScreen(int mx, int my, float parcial) {
        drawRect(0, 0, width, height, 0x44000000);
        Hud.dibujar(true);
        Config c = Config.get();
        boolean[] activo = {c.teclas, c.cps, c.fps};
        for (int i = 0; i < 3; i++) {
            int[] r = Hud.rect(i, mc);
            boolean encima = i == arrastrando || dentro(r, mx, my);
            int borde = encima ? 0xFFFFFFFF : (activo[i] ? 0x99FFFFFF : 0x55FF5555);
            contorno(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, borde);
            if (!activo[i]) {
                fontRendererObj.drawStringWithShadow(Hud.NOMBRES[i] + " (apagado)", r[0], r[1] + r[3] + 2, 0xFFFF7777);
            }
        }
        drawCenteredString(fontRendererObj, "Arrastra los elementos · ESC para terminar", width / 2, height - 20, 0xFFFFFF);
        super.drawScreen(mx, my, parcial);
    }

    private static void contorno(int x1, int y1, int x2, int y2, int color) {
        drawRect(x1, y1, x2, y1 + 1, color);
        drawRect(x1, y2 - 1, x2, y2, color);
        drawRect(x1, y1, x1 + 1, y2, color);
        drawRect(x2 - 1, y1, x2, y2, color);
    }

    private static boolean dentro(int[] r, int x, int y) {
        return x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3];
    }

    @Override
    protected void mouseClicked(int mx, int my, int boton) throws IOException {
        if (boton == 0) {
            for (int i = 0; i < 3; i++) {
                int[] r = Hud.rect(i, mc);
                if (dentro(r, mx, my)) {
                    arrastrando = i;
                    dx = mx - r[0];
                    dy = my - r[1];
                    return;
                }
            }
        }
        super.mouseClicked(mx, my, boton);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int boton, long tiempo) {
        if (arrastrando >= 0) {
            Hud.mover(arrastrando, mx - dx, my - dy);
            int[] r = Hud.rect(arrastrando, mc);
            Hud.mover(arrastrando, r[0], r[1]);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int estado) {
        arrastrando = -1;
        super.mouseReleased(mx, my, estado);
    }

    @Override
    protected void keyTyped(char c, int tecla) throws IOException {
        if (tecla == Keyboard.KEY_ESCAPE) {
            Config.get().guardar();
            mc.displayGuiScreen(padre);
            return;
        }
        super.keyTyped(c, tecla);
    }

    @Override
    public void onGuiClosed() {
        Config.get().guardar();
    }
}
