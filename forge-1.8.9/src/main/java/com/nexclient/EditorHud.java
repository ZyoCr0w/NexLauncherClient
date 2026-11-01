package com.nexclient;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Editor del HUD: arrastra cada elemento (se pega a las guías: centro, bordes, tercios y otros elementos; con Shift
 * se mueve libre), cambia su tamaño con la rueda o con la esquina, muévelo de a 1 píxel con las flechas y deshaz
 * con Ctrl+Z. Al hacer clic aparece su panel con escala, fondo, borde, colores y sombra.
 */
public class EditorHud extends GuiScreen {
    private static final int[] REJILLAS = {0, 5, 10, 20};
    private final GuiScreen padre;
    private final List<Runnable> acciones = new ArrayList<Runnable>();
    private String arrastrando, elegido, redimensionando;
    private int dx, dy, x0, y0;
    private boolean ocultos = true;
    private String antes;
    private final Deque<String> deshacer = new ArrayDeque<String>(), rehacer = new ArrayDeque<String>();
    private List<Guias.Linea> lineas = new ArrayList<Guias.Linea>();

    public EditorHud(GuiScreen padre) {
        this.padre = padre;
    }

    private static String textoRejilla(int v) {
        return "Cuadrícula: " + (v == 0 ? "No" : v + " px");
    }

    @Override
    public void initGui() {
        buttonList.clear();
        acciones.clear();
        Keyboard.enableRepeatEvents(true);
        final Config c = Config.get();
        int y = height - 24, x = width / 2 - 187;
        boton(x, y, 46, "Listo", new Runnable() {
            public void run() { cerrar(); }
        });
        final GuiButton[] ver = new GuiButton[2];
        ver[0] = boton(x + 50, y, 80, "Ocultos: " + (ocultos ? "Sí" : "No"), new Runnable() {
            public void run() {
                ocultos = !ocultos;
                ver[0].displayString = "Ocultos: " + (ocultos ? "Sí" : "No");
            }
        });
        ver[1] = boton(x + 134, y, 100, textoRejilla(c.rejilla), new Runnable() {
            public void run() {
                siguienteRejilla();
                ver[1].displayString = textoRejilla(c.rejilla);
            }
        });
        boton(x + 238, y, 20, "<", new Runnable() {
            public void run() { deshacer(); }
        }).enabled = !deshacer.isEmpty();
        boton(x + 260, y, 20, ">", new Runnable() {
            public void run() { rehacer(); }
        }).enabled = !rehacer.isEmpty();
        boton(x + 284, y, 90, "Restablecer todo", new Runnable() {
            public void run() {
                guardarParaDeshacer();
                for (String id : Hud.IDS) {
                    Hud.restablecer(id);
                }
                initGui();
            }
        });
        if (elegido == null) {
            return;
        }
        final Config.Estilo e = c.estilo(elegido);
        int[] r = Hud.rect(elegido, mc);
        int px = r[0] + r[2] / 2 < width / 2 ? width - 156 : 6;
        int py = 24;
        final String id = elegido;
        final GuiButton[] ref = new GuiButton[7];
        ref[0] = boton(px, py, 150, "Mostrar: " + (Hud.activo(c, id) ? "Sí" : "No"), new Runnable() {
            public void run() {
                guardarParaDeshacer();
                boolean v = !Hud.activo(c, id);
                Hud.poner(c, id, v);
                ref[0].displayString = "Mostrar: " + (v ? "Sí" : "No");
            }
        });
        py += 22;
        slider(px, py, "Escala: ", "%", 50, 300, e.escala * 100, new GuiSlider.ISlider() {
            public void onChangeSliderValue(GuiSlider s) { e.escala = (float) (Math.round(s.getValue() / 5) * 5 / 100.0); }
        });
        py += 22;
        slider(px, py, "Fondo: ", "%", 0, 100, e.fondo, new GuiSlider.ISlider() {
            public void onChangeSliderValue(GuiSlider s) { e.fondo = (int) Math.round(s.getValue()); }
        });
        py += 22;
        ref[1] = boton(px, py, 150, "Texto: " + Config.NOMBRES_COLOR[e.colorTexto], new Runnable() {
            public void run() {
                e.colorTexto = (e.colorTexto + 1) % Config.COLORES.length;
                ref[1].displayString = "Texto: " + Config.NOMBRES_COLOR[e.colorTexto];
            }
        });
        py += 22;
        ref[5] = boton(px, py, 150, "Arcoíris: " + (e.arcoiris ? "Sí" : "No"), new Runnable() {
            public void run() {
                e.arcoiris = !e.arcoiris;
                ref[5].displayString = "Arcoíris: " + (e.arcoiris ? "Sí" : "No");
            }
        });
        py += 22;
        ref[2] = boton(px, py, 150, "Sombra: " + (e.sombra ? "Sí" : "No"), new Runnable() {
            public void run() {
                e.sombra = !e.sombra;
                ref[2].displayString = "Sombra: " + (e.sombra ? "Sí" : "No");
            }
        });
        py += 22;
        ref[3] = boton(px, py, 150, "Borde: " + (e.borde ? "Sí" : "No"), new Runnable() {
            public void run() {
                e.borde = !e.borde;
                ref[3].displayString = "Borde: " + (e.borde ? "Sí" : "No");
            }
        });
        py += 22;
        ref[4] = boton(px, py, 150, "Color del borde: " + Config.NOMBRES_COLOR[e.colorBorde], new Runnable() {
            public void run() {
                e.colorBorde = (e.colorBorde + 1) % Config.COLORES.length;
                ref[4].displayString = "Color del borde: " + Config.NOMBRES_COLOR[e.colorBorde];
            }
        });
        py += 22;
        boton(px, py, 150, "Restablecer", new Runnable() {
            public void run() {
                guardarParaDeshacer();
                Hud.restablecer(id);
                initGui();
            }
        });
    }

    private void siguienteRejilla() {
        Config c = Config.get();
        int i = 0;
        while (i < REJILLAS.length && REJILLAS[i] != c.rejilla) {
            i++;
        }
        c.rejilla = REJILLAS[(i + 1) % REJILLAS.length];
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

    // ---- deshacer / rehacer
    private boolean[] activos() {
        Config c = Config.get();
        boolean[] a = new boolean[Hud.IDS.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = Hud.activo(c, Hud.IDS[i]);
        }
        return a;
    }

    private String instantanea() {
        return Config.get().instantaneaHud(activos());
    }

    private void guardarParaDeshacer() {
        guardarParaDeshacer(instantanea());
    }

    private void guardarParaDeshacer(String estado) {
        if (!deshacer.isEmpty() && deshacer.peek().equals(estado)) {
            return;
        }
        deshacer.push(estado);
        while (deshacer.size() > 60) {
            deshacer.removeLast();
        }
        rehacer.clear();
    }

    private void volver(Deque<String> de, Deque<String> a) {
        if (de.isEmpty()) {
            return;
        }
        a.push(instantanea());
        boolean[] v = Config.get().restaurarHud(de.pop());
        if (v != null) {
            for (int i = 0; i < v.length && i < Hud.IDS.length; i++) {
                Hud.poner(Config.get(), Hud.IDS[i], v[i]);
            }
        }
        initGui();
    }

    private void deshacer() {
        volver(deshacer, rehacer);
    }

    private void rehacer() {
        volver(rehacer, deshacer);
    }

    // ---- dibujo
    @Override
    public void drawScreen(int mx, int my, float parcial) {
        drawRect(0, 0, width, height, 0x55000000);
        Config c = Config.get();
        if (c.rejilla > 0) {
            for (int x = c.rejilla; x < width; x += c.rejilla) {
                drawRect(x, 0, x + 1, height, x % (c.rejilla * 5) == 0 ? 0x30FFFFFF : 0x16FFFFFF);
            }
            for (int y = c.rejilla; y < height; y += c.rejilla) {
                drawRect(0, y, width, y + 1, y % (c.rejilla * 5) == 0 ? 0x30FFFFFF : 0x16FFFFFF);
            }
        }
        drawRect(width / 2, 0, width / 2 + 1, height, 0x22FF55FF);
        drawRect(0, height / 2, width, height / 2 + 1, 0x22FF55FF);
        for (Guias.Linea l : lineas) {
            if (l.vertical) {
                drawRect(l.pos, l.desde, l.pos + 1, l.hasta, l.color);
            } else {
                drawRect(l.desde, l.pos, l.hasta, l.pos + 1, l.color);
            }
        }
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
                int[] q = esquina(r);
                drawRect(q[0], q[1], q[0] + q[2], q[1] + q[3], sel ? 0xFFFFFF55 : 0xFFFFFFFF);
            }
            if (id.equals(arrastrando) || id.equals(redimensionando)) {
                String info = r[0] + ", " + r[1] + "  ·  " + r[2] + "×" + r[3] + "  ·  "
                        + Math.round(c.estilo(id).escala * 100) + "%";
                int ty = r[1] + r[3] + 4 + 9 < height - 28 ? r[1] + r[3] + 4 : Math.max(0, r[1] - 12);
                drawRect(r[0] - 1, ty - 1, r[0] + fontRendererObj.getStringWidth(info) + 2, ty + 9, 0xAA000000);
                fontRendererObj.drawString(info, r[0] + 1, ty, 0xFFFFFFFF);
            } else if (sel || encima) {
                fontRendererObj.drawStringWithShadow(Hud.nombre(id) + (activo ? "" : " (oculto)"), r[0],
                        Math.max(0, r[1] - 10), 0xFFFFFFFF);
            }
        }
        drawCenteredString(fontRendererObj, "Arrastra: mover (Shift: libre) · Rueda o esquina: tamaño · Flechas: 1 px · Ctrl+Z",
                width / 2, 6, 0xFFFFFF);
        super.drawScreen(mx, my, parcial);
    }

    private static int[] esquina(int[] r) {
        return new int[]{r[0] + r[2] - 2, r[1] + r[3] - 2, 5, 5};
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

    private List<int[]> otros(String id) {
        Config c = Config.get();
        List<int[]> l = new ArrayList<int[]>();
        for (String o : Hud.IDS) {
            if (!o.equals(id) && Hud.activo(c, o)) {
                l.add(Hud.rect(o, mc));
            }
        }
        return l;
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

    // ---- ratón y teclado
    @Override
    protected void mouseClicked(int mx, int my, int boton) throws IOException {
        if (sobreBoton(mx, my)) {
            super.mouseClicked(mx, my, boton);
            return;
        }
        if (boton != 0) {
            return;
        }
        for (String id : new String[]{elegido, bajo(mx, my)}) {  // primero la esquina del elegido
            if (id != null) {
                int[] r = Hud.rect(id, mc);
                int[] q = esquina(r);
                if (mx >= q[0] - 1 && my >= q[1] - 1 && mx < q[0] + q[2] + 1 && my < q[1] + q[3] + 1) {
                    antes = instantanea();
                    redimensionando = id;
                    x0 = r[0];
                    y0 = r[1];
                    return;
                }
            }
        }
        String id = bajo(mx, my);
        if (id != null) {
            int[] r = Hud.rect(id, mc);
            antes = instantanea();
            arrastrando = id;
            dx = mx - r[0];
            dy = my - r[1];
        }
        if (id == null ? elegido != null : !id.equals(elegido)) {
            elegido = id;
            initGui();
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int boton, long tiempo) {
        if (redimensionando != null) {
            Config c = Config.get();
            int[] b = Hud.base(redimensionando, mc, c);
            float escala = (float) Math.max(0.5, Math.min(3.0, (mx - x0) / (double) Math.max(1, b[0])));
            c.estilo(redimensionando).escala = Math.round(escala * 20) / 20f;
            Hud.mover(redimensionando, x0, y0);
        } else if (arrastrando != null) {
            int[] r = Hud.rect(arrastrando, mc);
            Guias.Resultado a = Guias.ajustar(mx - dx, my - dy, r[2], r[3], width, height, otros(arrastrando),
                    !isShiftKeyDown(), Config.get().rejilla);
            Hud.mover(arrastrando, a.x, a.y);
            lineas = a.lineas;
        } else {
            super.mouseClickMove(mx, my, boton, tiempo);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int estado) {
        if (arrastrando != null || redimensionando != null) {
            if (antes != null && !antes.equals(instantanea())) {
                guardarParaDeshacer(antes);
            }
            antes = null;
            arrastrando = null;
            redimensionando = null;
            lineas = new ArrayList<Guias.Linea>();
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
                guardarParaDeshacer();
                int[] r = Hud.rect(id, mc);
                Config.Estilo e = Config.get().estilo(id);
                e.escala = Math.max(0.5f, Math.min(3f, Math.round((e.escala + (rueda > 0 ? 0.1f : -0.1f)) * 20) / 20f));
                Hud.mover(id, r[0], r[1]);
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
        if (isCtrlKeyDown() && tecla == Keyboard.KEY_Z) {
            if (isShiftKeyDown()) {
                rehacer();
            } else {
                deshacer();
            }
            return;
        }
        if (isCtrlKeyDown() && tecla == Keyboard.KEY_Y) {
            rehacer();
            return;
        }
        if (tecla == Keyboard.KEY_G && !isCtrlKeyDown()) {
            siguienteRejilla();
            initGui();
            return;
        }
        if (elegido != null && (tecla == Keyboard.KEY_LEFT || tecla == Keyboard.KEY_RIGHT || tecla == Keyboard.KEY_UP
                || tecla == Keyboard.KEY_DOWN)) {
            guardarParaDeshacer();
            int paso = isShiftKeyDown() ? 10 : 1;
            int[] r = Hud.rect(elegido, mc);
            int x = r[0] + (tecla == Keyboard.KEY_LEFT ? -paso : tecla == Keyboard.KEY_RIGHT ? paso : 0);
            int y = r[1] + (tecla == Keyboard.KEY_UP ? -paso : tecla == Keyboard.KEY_DOWN ? paso : 0);
            Hud.mover(elegido, Math.max(0, Math.min(x, width - r[2])), Math.max(0, Math.min(y, height - r[3])));
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
        Keyboard.enableRepeatEvents(false);
        Config.get().guardar();
    }
}
