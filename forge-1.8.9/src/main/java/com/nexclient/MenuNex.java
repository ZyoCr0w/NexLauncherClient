package com.nexclient;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Menú de NexClient (Shift derecho): HUD, Blockhit y Partículas. */
public class MenuNex extends GuiScreen {
    private static final String[] PAGINAS = {"HUD", "Blockhit", "Partículas"};
    private static int pagina = 0;
    private final GuiScreen padre;
    private final List<Runnable> acciones = new ArrayList<Runnable>();
    private int fila;
    private String ayuda = "";

    public MenuNex(GuiScreen padre) {
        this.padre = padre;
    }

    interface Lector {
        boolean leer();
    }

    interface Escritor {
        void poner(boolean v);
    }

    interface Numero {
        void poner(double v);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        acciones.clear();
        final Config c = Config.get();
        int cx = width / 2;
        for (int i = 0; i < PAGINAS.length; i++) {
            final int k = i;
            GuiButton b = boton(cx - 155 + i * 105, 32, 100, i == pagina ? "§l" + PAGINAS[i] : PAGINAS[i], new Runnable() {
                public void run() {
                    pagina = k;
                    initGui();
                }
            });
            b.enabled = i != pagina;
        }
        fila = 0;
        if (pagina == 0) {
            ayuda = "Shift derecho abre este menú en cualquier momento.";
            interruptor("Teclas en pantalla", c.teclas, new Escritor() { public void poner(boolean v) { c.teclas = v; } });
            interruptor("CPS en las teclas", c.teclasCps, new Escritor() { public void poner(boolean v) { c.teclasCps = v; } });
            interruptor("Botones del ratón", c.teclasRaton, new Escritor() { public void poner(boolean v) { c.teclasRaton = v; } });
            interruptor("Barra espaciadora", c.teclasEspacio, new Escritor() { public void poner(boolean v) { c.teclasEspacio = v; } });
            interruptor("Contador de CPS", c.cps, new Escritor() { public void poner(boolean v) { c.cps = v; } });
            interruptor("Contador de FPS", c.fps, new Escritor() { public void poner(boolean v) { c.fps = v; } });
            deslizador("Tamaño: ", "%", 50, 250, c.teclasEscala * 100, new Numero() {
                public void poner(double v) { c.teclasEscala = (float) (Math.round(v / 5) * 5 / 100.0); } });
            final GuiScreen yo = this;
            boton(x(), y(), 150, "Mover elementos del HUD…", new Runnable() {
                public void run() { mc.displayGuiScreen(new EditorHud(yo)); } });
        } else if (pagina == 1) {
            ayuda = "Blockhit: golpea y bloquea con la espada. «Estilo 1.7» mueve la espada al golpear bloqueando.";
            interruptor("Blockhit visual", c.blockhit, new Escritor() { public void poner(boolean v) { c.blockhit = v; } });
            interruptor("Estilo 1.7", c.estilo17, new Escritor() { public void poner(boolean v) { c.estilo17 = v; } });
            interruptor("Sonido de blockhit", c.sonido, new Escritor() { public void poner(boolean v) { c.sonido = v; } });
            final GuiButton[] ref = new GuiButton[1];
            ref[0] = boton(x(), y(), 150, "Sonido: " + Config.nombreSonido(c.sonidoId), new Runnable() {
                public void run() {
                    int i = (Math.max(0, Config.indiceSonido(c.sonidoId)) + 1) % Config.SONIDOS.length;
                    c.sonidoId = Config.SONIDOS[i];
                    ref[0].displayString = "Sonido: " + Config.nombreSonido(c.sonidoId);
                    Efectos.sonar();
                }
            });
            interruptor("Sonar en cada golpe", c.sonidoCadaGolpe, new Escritor() { public void poner(boolean v) { c.sonidoCadaGolpe = v; } });
            deslizador("Volumen: ", "%", 0, 100, c.volumen * 100, new Numero() {
                public void poner(double v) { c.volumen = (float) (Math.round(v) / 100.0); } });
            deslizador("Tono: ", "%", 50, 200, c.tono * 100, new Numero() {
                public void poner(double v) { c.tono = (float) (Math.round(v) / 100.0); } });
            boton(x(), y(), 150, "Probar sonido", new Runnable() { public void run() { Efectos.sonar(); } });
        } else {
            ayuda = "«Siempre crítico» y «Siempre filo» son solo visuales: no cambian el daño.";
            interruptor("Multiplicar partículas", c.particulas, new Escritor() { public void poner(boolean v) { c.particulas = v; } });
            deslizador("Multiplicador: x", "", 1, 10, c.multiplicador, new Numero() {
                public void poner(double v) { c.multiplicador = (int) Math.round(v); } });
            interruptor("Siempre crítico", c.siempreCritico, new Escritor() { public void poner(boolean v) { c.siempreCritico = v; } });
            interruptor("Siempre filo", c.siempreFilo, new Escritor() { public void poner(boolean v) { c.siempreFilo = v; } });
        }
        boton(cx - 100, height - 28, 200, "Listo", new Runnable() { public void run() { cerrar(); } });
    }

    private int x() {
        return width / 2 + (fila % 2 == 0 ? -155 : 5);
    }

    private int y() {
        int y = 64 + (fila / 2) * 24;
        fila++;
        return y;
    }

    private GuiButton boton(int x, int y, int w, String texto, Runnable accion) {
        GuiButton b = new GuiButton(acciones.size(), x, y, w, 20, texto);
        acciones.add(accion);
        buttonList.add(b);
        return b;
    }

    private void interruptor(final String texto, boolean valor, final Escritor poner) {
        final boolean[] v = {valor};
        final GuiButton[] ref = new GuiButton[1];
        ref[0] = boton(x(), y(), 150, texto + ": " + (valor ? "Sí" : "No"), new Runnable() {
            public void run() {
                v[0] = !v[0];
                poner.poner(v[0]);
                ref[0].displayString = texto + ": " + (v[0] ? "Sí" : "No");
            }
        });
    }

    private void deslizador(String prefijo, String sufijo, double min, double max, double valor, final Numero poner) {
        int id = acciones.size();
        acciones.add(null);
        GuiSlider s = new GuiSlider(id, x(), y(), 150, 20, prefijo, sufijo, min, max, valor, false, true,
                new GuiSlider.ISlider() {
                    public void onChangeSliderValue(GuiSlider slider) {
                        poner.poner(slider.getValue());
                    }
                });
        buttonList.add(s);
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= 0 && b.id < acciones.size() && acciones.get(b.id) != null) {
            acciones.get(b.id).run();
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

    @Override
    public void drawScreen(int mx, int my, float parcial) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "§lNexClient", width / 2, 12, 0xFFFFFF);
        drawCenteredString(fontRendererObj, ayuda, width / 2, height - 44, 0xAAAAAA);
        super.drawScreen(mx, my, parcial);
    }
}
