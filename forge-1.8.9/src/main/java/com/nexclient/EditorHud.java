package com.nexclient;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/** Arrastra cada elemento del HUD a donde quieras. */
public class EditorHud extends GuiScreen {
    private final GuiScreen padre;
    private String arrastrando;
    private int dx, dy;

    public EditorHud(GuiScreen padre) {
        this.padre = padre;
    }

    @Override
    public void drawScreen(int mx, int my, float parcial) {
        drawRect(0, 0, width, height, 0x44000000);
        Hud.dibujar(true);
        Config c = Config.get();
        int activos = 0;
        for (String id : Hud.IDS) {
            if (!Hud.activo(c, id)) {
                continue;
            }
            activos++;
            int[] r = Hud.rect(id, mc);
            boolean encima = id.equals(arrastrando) || dentro(r, mx, my);
            contorno(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, encima ? 0xFFFFFFFF : 0x88FFFFFF);
            if (encima) {
                fontRendererObj.drawStringWithShadow(Hud.nombre(id), r[0], Math.max(0, r[1] - 10), 0xFFFFFFFF);
            }
        }
        drawCenteredString(fontRendererObj, activos == 0 ? "Activa elementos en el menú de NexClient (pestañas HUD e Info)"
                : "Arrastra los elementos · ESC para terminar", width / 2, height - 20, 0xFFFFFF);
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
            Config c = Config.get();
            for (int i = Hud.IDS.length - 1; i >= 0; i--) {
                String id = Hud.IDS[i];
                int[] r = Hud.rect(id, mc);
                if (Hud.activo(c, id) && dentro(r, mx, my)) {
                    arrastrando = id;
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
        if (arrastrando != null) {
            Hud.mover(arrastrando, mx - dx, my - dy);
            int[] r = Hud.rect(arrastrando, mc);
            Hud.mover(arrastrando, r[0], r[1]);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int estado) {
        arrastrando = null;
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
