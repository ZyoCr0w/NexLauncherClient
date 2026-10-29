package com.nexclient;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor del HUD estilo Lunar: arrastra cada elemento, la rueda cambia su tamaño y al hacer clic aparece su
 * panel con escala, fondo, borde, colores y sombra.
 */
public class EditorHud extends GuiScreen {
    private final GuiScreen padre;
    private final List<Runnable> acciones = new ArrayList<Runnable>();
    private String arrastrando, elegido;
    private int dx, dy;
    private boolean ocultos = true;

    public EditorHud(GuiScreen padre) {
        this.padre = padre;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        acciones.clear();
        final Config c = Config.get();
        boton(width / 2 - 75, height - 24, 70, "Listo", new Runnable() {
            public void run() { cerrar(); }
        });
        final GuiButton[] ver = new GuiButton[1];
        ver[0] = boton(width / 2 + 5, height - 24, 140, "Ver ocultos: " + (ocultos ? "Sí" : "No"), new Runnable() {
            public void run() {
                ocultos = !ocultos;
                ver[0].displayString = "Ver ocultos: " + (ocultos ? "Sí" : "No");
            }
        });
        if (elegido == null) {
            return;
        }
        final Config.Estilo e = c.estilo(elegido);
        int[] r = Hud.rect(elegido, mc);
        int x = r[0] + r[2] / 2 < width / 2 ? width - 156 : 6;
        int y = 24;
        final String id = elegido;
        final GuiButton[] ref = new GuiButton[6];
        ref[0] = boton(x, y, 150, "Mostrar: " + (Hud.activo(c, id) ? "Sí" : "No"), new Runnable() {
            public void run() {
                boolean v = !Hud.activo(c, id);
                Hud.poner(c, id, v);
                ref[0].displayString = "Mostrar: " + (v ? "Sí" : "No");
            }
        });
        y += 22;
        slider(x, y, "Escala: ", "%", 50, 300, e.escala * 100, new GuiSlider.ISlider() {
            public void onChangeSliderValue(GuiSlider s) { e.escala = (float) (Math.round(s.getValue() / 5) * 5 / 100.0); }
        });
        y += 22;
        slider(x, y, "Fondo: ", "%", 0, 100, e.fondo, new GuiSlider.ISlider() {
            public void onChangeSliderValue(GuiSlider s) { e.fondo = (int) Math.round(s.getValue()); }
        });
        y += 22;
        ref[1] = boton(x, y, 150, "Texto: " + Config.NOMBRES_COLOR[e.colorTexto], new Runnable() {
            public void run() {
                e.colorTexto = (e.colorTexto + 1) % Config.COLORES.length;
                ref[1].displayString = "Texto: " + Config.NOMBRES_COLOR[e.colorTexto];
            }
        });
        y += 22;
        ref[2] = boton(x, y, 150, "Sombra: " + (e.sombra ? "Sí" : "No"), new Runnable() {
            public void run() {
                e.sombra = !e.sombra;
                ref[2].displayString = "Sombra: " + (e.sombra ? "Sí" : "No");
            }
        });
        y += 22;
        ref[3] = boton(x, y, 150, "Borde: " + (e.borde ? "Sí" : "No"), new Runnable() {
            public void run() {
                e.borde = !e.borde;
                ref[3].displayString = "Borde: " + (e.borde ? "Sí" : "No");
            }
        });
        y += 22;
        ref[4] = boton(x, y, 150, "Color del borde: " + Config.NOMBRES_COLOR[e.colorBorde], new Runnable() {
            public void run() {
                e.colorBorde = (e.colorBorde + 1) % Config.COLORES.length;
                ref[4].displayString = "Color del borde: " + Config.NOMBRES_COLOR[e.colorBorde];
            }
        });
        y += 22;
        boton(x, y, 150, "Restablecer", new Runnable() {
            public void run() {
                Hud.restablecer(id);
                initGui();
            }
        });
    }

    private GuiButton boton(int x, int y, int w, String texto, Runnable accion) {
        GuiButton b = new GuiButton(acciones.size(), x, y, w, 20, texto);
        acciones.add(accion);
        buttonList.add(b);
        return b;
    }

    private void slider(int x, int y, String prefijo, String sufijo, double min, double max, double valor,
                        GuiSlider.ISlider al) {
        int id = acciones.size();
        acciones.add(null);
        buttonList.add(new GuiSlider(id, x, y, 150, 20, prefijo, sufijo, min, max, valor, false, true, al));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= 0 && b.id < acciones.size() && acciones.get(b.id) != null) {
            acciones.get(b.id).run();
        }
    }

    @Override
    public void drawScreen(int mx, int my, float parcial) {
        drawRect(0, 0, width, height, 0x55000000);
        Config c = Config.get();
        for (String id : Hud.IDS) {
            boolean activo = Hud.activo(c, id);
            if (!activo && !ocultos) {
                continue;
            }
            int[] r = Hud.rect(id, mc);
            if (activo) {
                Hud.dibujarUno(mc, c, id, true, 0.016f);
            } else {
                drawRect(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x33FF5555);
                fontRendererObj.drawString(Hud.nombre(id), r[0] + 2, r[1] + 2, 0xFFFF9999);
            }
            boolean sel = id.equals(elegido);
            boolean encima = id.equals(arrastrando) || dentro(r, mx, my);
            Hud.contorno(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1,
                    sel ? 0xFFFFFF55 : encima ? 0xFFFFFFFF : 0x66FFFFFF);
            if (sel || encima) {
                fontRendererObj.drawStringWithShadow(Hud.nombre(id) + (activo ? "" : " (oculto)"), r[0],
                        Math.max(0, r[1] - 10), 0xFFFFFFFF);
            }
        }
        drawCenteredString(fontRendererObj, "Clic: elegir · Arrastra: mover · Rueda: tamaño", width / 2, 6, 0xFFFFFF);
        super.drawScreen(mx, my, parcial);
    }

    private static boolean dentro(int[] r, int x, int y) {
        return x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3];
    }

    private String bajo(int mx, int my) {
        Config c = Config.get();
        for (int i = Hud.IDS.length - 1; i >= 0; i--) {
            String id = Hud.IDS[i];
            if ((Hud.activo(c, id) || ocultos) && dentro(Hud.rect(id, mc), mx, my)) {
                return id;
            }
        }
        return null;
    }

    private boolean sobreBoton(int mx, int my) {
        for (GuiButton b : buttonList) {
            if (b.visible && mx >= b.xPosition && my >= b.yPosition && mx < b.xPosition + b.width
                    && my < b.yPosition + b.height) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void mouseClicked(int mx, int my, int boton) throws IOException {
        if (sobreBoton(mx, my)) {
            super.mouseClicked(mx, my, boton);
            return;
        }
        if (boton == 0) {
            String id = bajo(mx, my);
            if (id != null) {
                int[] r = Hud.rect(id, mc);
                arrastrando = id;
                dx = mx - r[0];
                dy = my - r[1];
            }
            if (id == null ? elegido != null : !id.equals(elegido)) {
                elegido = id;
                initGui();
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int boton, long tiempo) {
        if (arrastrando != null) {
            Hud.mover(arrastrando, mx - dx, my - dy);
            int[] r = Hud.rect(arrastrando, mc);
            Hud.mover(arrastrando, r[0], r[1]);
        } else {
            super.mouseClickMove(mx, my, boton, tiempo);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int estado) {
        if (arrastrando != null) {
            arrastrando = null;
            initGui();
        }
        super.mouseReleased(mx, my, estado);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int rueda = Mouse.getEventDWheel();
        if (rueda != 0) {
            int mx = Mouse.getEventX() * width / mc.displayWidth;
            int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            String id = bajo(mx, my);
            if (id != null) {
                Config.Estilo e = Config.get().estilo(id);
                e.escala = Math.max(0.5f, Math.min(3f, Math.round((e.escala + (rueda > 0 ? 0.1f : -0.1f)) * 20) / 20f));
                if (id.equals(elegido)) {
                    initGui();
                }
            }
        }
    }

    @Override
    protected void keyTyped(char c, int tecla) throws IOException {
        if (tecla == Keyboard.KEY_ESCAPE) {
            cerrar();
            return;
        }
        super.keyTyped(c, tecla);
    }

    private void cerrar() {
        Config.get().guardar();
        mc.displayGuiScreen(padre);
    }

    @Override
    public void onGuiClosed() {
        Config.get().guardar();
    }
}
