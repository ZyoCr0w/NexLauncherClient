package com.nexclient;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Editor de la mira: vista previa grande, 13 formas, tamaño, grosor, separación, opacidad, colores, borde, mira
 * dinámica, color al apuntar, destello, 5 ranuras guardadas y un código para compartirla. En «Propia» la vista
 * previa es una cuadrícula para pintarla píxel por píxel (clic: pintar, clic derecho: borrar).
 */
public class EditorMira extends GuiScreen {
    private static final int[] PALETA = {0xFFFFFF, 0xAAAAAA, 0x000000, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x00AA00,
            0x55FFFF, 0x5555FF, 0xFF55FF, 0xAA00AA};
    private static final int CELDA = 7;
    private final GuiScreen padre;
    private final List<Runnable> acciones = new ArrayList<Runnable>();
    private int vx, vy, vt, colA, colB, fila0;
    private int pintando = -1;
    private String aviso = "";
    private long avisoHasta;

    interface Escritor {
        void poner(boolean v);
    }

    interface Numero {
        void poner(int v);
    }

    public EditorMira(GuiScreen padre) {
        this.padre = padre;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        acciones.clear();
        final Config c = Config.get();
        c.mira = true;
        vt = Miras.LADO * CELDA + 4;
        int total = vt + 10 + 150 + 4 + 150;
        vx = Math.max(4, width / 2 - total / 2);
        vy = 40;
        colA = vx + vt + 10;
        colB = colA + 154;
        fila0 = 40;
        int y = fila0;
        boton(colA, y, 20, "<", new Runnable() { public void run() { estilo(-1); } });
        boton(colA + 22, y, 106, Miras.ESTILOS[c.miraEstilo], new Runnable() { public void run() { estilo(1); } });
        boton(colA + 130, y, 20, ">", new Runnable() { public void run() { estilo(1); } });
        y += 22;
        slider(colA, y, "Tamaño: ", 1, 20, c.miraTamano, new Numero() { public void poner(int v) { c.miraTamano = v; } });
        y += 22;
        slider(colA, y, "Grosor: ", 1, 6, c.miraGrosor, new Numero() { public void poner(int v) { c.miraGrosor = v; } });
        y += 22;
        slider(colA, y, "Separación: ", 0, 12, c.miraHueco, new Numero() { public void poner(int v) { c.miraHueco = v; } });
        y += 22;
        slider(colA, y, "Opacidad %: ", 10, 100, c.miraOpacidad, new Numero() { public void poner(int v) { c.miraOpacidad = v; } });
        y = filaColor() + 12;
        interruptor(colA, y, "Borde", c.miraBorde, new Escritor() { public void poner(boolean v) { c.miraBorde = v; } });
        y += 22;
        slider(colA, y, "Grosor del borde: ", 1, 3, c.miraBordeGrosor, new Numero() { public void poner(int v) { c.miraBordeGrosor = v; } });
        y = fila0;
        interruptor(colB, y, "Arcoíris", c.miraArcoiris, new Escritor() { public void poner(boolean v) { c.miraArcoiris = v; } });
        y += 22;
        interruptor(colB, y, "Dinámica", c.miraDinamica, new Escritor() { public void poner(boolean v) { c.miraDinamica = v; } });
        y += 22;
        interruptor(colB, y, "Color al apuntar", c.miraObjetivo, new Escritor() { public void poner(boolean v) { c.miraObjetivo = v; } });
        y = filaObjetivo() + 12;
        interruptor(colB, y, "Destello al golpear", c.miraDestello, new Escritor() { public void poner(boolean v) { c.miraDestello = v; } });
        y += 22;
        interruptor(colB, y, "En tercera persona", c.miraTercera, new Escritor() { public void poner(boolean v) { c.miraTercera = v; } });
        y += 22;
        interruptor(colB, y, "Ocultar con arco", c.miraOcultarArco, new Escritor() { public void poner(boolean v) { c.miraOcultarArco = v; } });
        y = filaBorde();
        boton(colB, y, 74, "Copiar código", new Runnable() {
            public void run() {
                setClipboardString(Miras.codigo(Config.get()));
                avisar("Código copiado: pégalo a un amigo o en NexLauncher.");
            }
        });
        boton(colB + 76, y, 74, "Pegar código", new Runnable() {
            public void run() {
                if (Miras.aplicar(Config.get(), getClipboardString())) {
                    avisar("Mira cargada.");
                    initGui();
                } else {
                    avisar("Lo copiado no es un código de mira (empieza con NXM:).");
                }
            }
        });
        boton(width / 2 - 100, height - 26, 200, "Listo", new Runnable() { public void run() { cerrar(); } });
    }

    private int filaColor() {
        return fila0 + 110;
    }

    private int filaObjetivo() {
        return fila0 + 66;
    }

    private int filaBorde() {
        return fila0 + 166;
    }

    private int filaRanuras() {
        return fila0 + 146;
    }

    private void estilo(int paso) {
        Config c = Config.get();
        c.miraEstilo = ((c.miraEstilo + paso) % Miras.ESTILOS.length + Miras.ESTILOS.length) % Miras.ESTILOS.length;
        initGui();
    }

    private void avisar(String texto) {
        aviso = texto;
        avisoHasta = System.currentTimeMillis() + 3500;
    }

    private GuiButton boton(int x, int y, int w, String texto, Runnable accion) {
        GuiButton b = new GuiButton(acciones.size(), x, y, w, 20, texto);
        acciones.add(accion);
        buttonList.add(b);
        return b;
    }

    private void interruptor(int x, int y, final String texto, boolean valor, final Escritor poner) {
        final boolean[] v = {valor};
        final GuiButton[] ref = new GuiButton[1];
        ref[0] = boton(x, y, 150, texto + ": " + (valor ? "Sí" : "No"), new Runnable() {
            public void run() {
                v[0] = !v[0];
                poner.poner(v[0]);
                ref[0].displayString = texto + ": " + (v[0] ? "Sí" : "No");
            }
        });
    }

    private void slider(int x, int y, String prefijo, int min, int max, int valor, final Numero poner) {
        int id = acciones.size();
        acciones.add(null);
        buttonList.add(new GuiSlider(id, x, y, 150, 20, prefijo, "", min, max, valor, false, true, new GuiSlider.ISlider() {
            public void onChangeSliderValue(GuiSlider s) { poner.poner((int) Math.round(s.getValue())); }
        }));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= 0 && b.id < acciones.size() && acciones.get(b.id) != null) {
            acciones.get(b.id).run();
        }
    }

    private int ladoPaleta(String titulo) {
        return Math.max(6, Math.min(10, (150 - fontRendererObj.getStringWidth(titulo) - 4) / PALETA.length - 1));
    }

    private void paleta(int x, int y, int actual, String titulo) {
        fontRendererObj.drawString(titulo, x, y + 2, 0xFFAAAAAA);
        int x0 = x + fontRendererObj.getStringWidth(titulo) + 4, lado = ladoPaleta(titulo);
        for (int i = 0; i < PALETA.length; i++) {
            int px = x0 + i * (lado + 1);
            if (PALETA[i] == (actual & 0xFFFFFF)) {
                drawRect(px - 1, y, px + lado + 1, y + lado + 2, 0xFFFFFF55);
            }
            drawRect(px, y + 1, px + lado, y + 1 + lado, 0xFF000000 | PALETA[i]);
        }
    }

    private int colorEn(int mx, int my, int x, int y, String titulo) {
        int x0 = x + fontRendererObj.getStringWidth(titulo) + 4, lado = ladoPaleta(titulo);
        if (my < y || my > y + lado + 1 || mx < x0) {
            return -1;
        }
        int i = (mx - x0) / (lado + 1);
        return i >= 0 && i < PALETA.length ? PALETA[i] : -1;
    }

    @Override
    public void drawScreen(int mx, int my, float parcial) {
        drawDefaultBackground();
        Config c = Config.get();
        drawCenteredString(fontRendererObj, "§lMira", width / 2, 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Así se ve en tu pantalla (vista previa al lado, más grande)", width / 2, 22, 0x888888);
        drawRect(vx, vy, vx + vt, vy + vt, 0xFF3A5A2A);
        drawRect(vx, vy + vt / 2, vx + vt, vy + vt, 0xFF6B4A2B);
        if (c.miraEstilo == Miras.PROPIA) {
            for (int i = 0; i < Miras.LADO * Miras.LADO; i++) {
                int x = vx + 2 + (i % Miras.LADO) * CELDA, y = vy + 2 + (i / Miras.LADO) * CELDA;
                boolean on = c.miraPixeles.charAt(i) == '1';
                drawRect(x, y, x + CELDA - 1, y + CELDA - 1, on ? (0xFF000000 | c.miraRGB)
                        : (i == Miras.LADO * Miras.LADO / 2 ? 0x55FF55FF : 0x33000000));
            }
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(vx + vt / 2f, vy + vt / 2f, 0);
            GlStateManager.scale(4f, 4f, 1f);
            Hud.mira(0, 0);
            GlStateManager.popMatrix();
        }
        drawRect(vx, vy + vt + 6, vx + vt, vy + vt + 46, 0xFF3A5A2A);
        Hud.mira(vx + vt / 2, vy + vt + 26);
        fontRendererObj.drawString("Tamaño real", vx, vy + vt + 49, 0xFF888888);
        paleta(colA, filaColor(), c.miraRGB, "Color");
        paleta(colB, filaObjetivo(), c.miraObjetivoRGB, "Al apuntar");
        paleta(colA, filaBorde(), c.miraBordeRGB, "Borde");
        int ry = filaRanuras();
        fontRendererObj.drawString("Ranuras:", colB, ry + 4, 0xFFAAAAAA);
        for (int i = 0; i < 5; i++) {
            int x = colB + 46 + i * 21;
            boolean llena = i < c.miraRanuras.size() && c.miraRanuras.get(i) != null && !c.miraRanuras.get(i).isEmpty();
            boolean encima = mx >= x && mx < x + 19 && my >= ry && my < ry + 16;
            drawRect(x, ry, x + 19, ry + 16, encima ? 0xFF555555 : 0xFF333333);
            drawCenteredString(fontRendererObj, (llena ? "§a" : "§7") + (i + 1), x + 10, ry + 4, 0xFFFFFF);
        }
        super.drawScreen(mx, my, parcial);
        if (System.currentTimeMillis() < avisoHasta) {
            drawCenteredString(fontRendererObj, aviso, width / 2, height - 40, 0x55FF55);
        } else if (c.miraEstilo == Miras.PROPIA) {
            drawCenteredString(fontRendererObj, "Clic: pintar · Clic derecho: borrar (el centro está marcado)", width / 2,
                    height - 40, 0x888888);
        }
        if (mx >= colB && my >= ry && my < ry + 16 && mx < colB + 46 + 5 * 21) {
            drawHoveringText(Arrays.asList("Clic: cargar", "Clic derecho: guardar aquí"), mx, my);
        }
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
        Config c = Config.get();
        int k;
        if ((k = colorEn(mx, my, colA, filaColor(), "Color")) >= 0) {
            c.miraRGB = k;
            c.miraArcoiris = false;
            initGui();
            return;
        }
        if ((k = colorEn(mx, my, colB, filaObjetivo(), "Al apuntar")) >= 0) {
            c.miraObjetivoRGB = k;
            return;
        }
        if ((k = colorEn(mx, my, colA, filaBorde(), "Borde")) >= 0) {
            c.miraBordeRGB = k;
            return;
        }
        int ry = filaRanuras();
        for (int i = 0; i < 5; i++) {
            int x = colB + 46 + i * 21;
            if (mx >= x && mx < x + 19 && my >= ry && my < ry + 16) {
                while (c.miraRanuras.size() <= i) {
                    c.miraRanuras.add("");
                }
                if (boton == 1) {
                    c.miraRanuras.set(i, Miras.codigo(c));
                    avisar("Mira guardada en la ranura " + (i + 1) + ".");
                } else if (!c.miraRanuras.get(i).isEmpty() && Miras.aplicar(c, c.miraRanuras.get(i))) {
                    avisar("Ranura " + (i + 1) + " cargada.");
                    initGui();
                } else {
                    avisar("La ranura " + (i + 1) + " está vacía: clic derecho para guardar ahí.");
                }
                return;
            }
        }
        if (c.miraEstilo == Miras.PROPIA && pintar(mx, my, boton == 0 ? 1 : 0)) {
            pintando = boton == 0 ? 1 : 0;
        }
    }

    private boolean pintar(int mx, int my, int valor) {
        int cx = (mx - vx - 2) / CELDA, cy = (my - vy - 2) / CELDA;
        if (mx < vx + 2 || my < vy + 2 || cx >= Miras.LADO || cy >= Miras.LADO) {
            return false;
        }
        Config c = Config.get();
        char[] p = c.miraPixeles.toCharArray();
        p[cy * Miras.LADO + cx] = valor == 1 ? '1' : '0';
        c.miraPixeles = new String(p);
        return true;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int boton, long tiempo) {
        if (pintando >= 0) {
            pintar(mx, my, pintando);
        } else {
            super.mouseClickMove(mx, my, boton, tiempo);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int estado) {
        pintando = -1;
        super.mouseReleased(mx, my, estado);
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
