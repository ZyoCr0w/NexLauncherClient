package com.nexclient;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Menú de NexClient (Shift derecho). */
public class MenuNex extends GuiScreen {
    private static final String[] PAGINAS = {"HUD", "Info", "Blockhit", "Juego", "Visual", "Mods"};
    private static int pagina = 0;
    private final GuiScreen padre;
    private final List<Runnable> acciones = new ArrayList<Runnable>();
    private int fila;
    private String ayuda = "";

    public MenuNex(GuiScreen padre) {
        this.padre = padre;
    }

    interface Escritor {
        void poner(boolean v);
    }

    interface Numero {
        void poner(double v);
    }

    interface Formato {
        String texto(double v);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        acciones.clear();
        final Config c = Config.get();
        int cx = width / 2;
        int ancho = 54;
        int x0 = cx - (PAGINAS.length * (ancho + 2)) / 2;
        for (int i = 0; i < PAGINAS.length; i++) {
            final int k = i;
            GuiButton b = boton(x0 + i * (ancho + 2), 28, ancho, i == pagina ? "§l" + PAGINAS[i] : PAGINAS[i],
                    new Runnable() {
                        public void run() {
                            pagina = k;
                            initGui();
                        }
                    });
            b.enabled = i != pagina;
        }
        fila = 0;
        if (pagina == 0) {
            ayuda = "Shift derecho o Esc → NexClient abren este menú. Zoom: tecla C (cámbiala en Controles).";
            final GuiScreen yo = this;
            boton(width / 2 - 155, y(), 310, "§lEditar HUD§r · posición, tamaño, fondo, borde y colores", new Runnable() {
                public void run() { mc.displayGuiScreen(new EditorHud(yo)); } });
            fila++;
            interruptor("Teclas en pantalla", c.teclas, new Escritor() { public void poner(boolean v) { c.teclas = v; } });
            interruptor("CPS en las teclas", c.teclasCps, new Escritor() { public void poner(boolean v) { c.teclasCps = v; } });
            interruptor("Botones del ratón", c.teclasRaton, new Escritor() { public void poner(boolean v) { c.teclasRaton = v; } });
            interruptor("Barra espaciadora", c.teclasEspacio, new Escritor() { public void poner(boolean v) { c.teclasEspacio = v; } });
            interruptor("Contador de CPS", c.cps, new Escritor() { public void poner(boolean v) { c.cps = v; } });
            interruptor("Contador de FPS", c.fps, new Escritor() { public void poner(boolean v) { c.fps = v; } });
        } else if (pagina == 1) {
            ayuda = "Activa lo que quieras ver y acomódalo con «Mover elementos del HUD».";
            interruptor("Coordenadas", c.coords, new Escritor() { public void poner(boolean v) { c.coords = v; } });
            interruptor("Dirección (N, S…)", c.coordsDireccion, new Escritor() { public void poner(boolean v) { c.coordsDireccion = v; } });
            interruptor("Ping", c.ping, new Escritor() { public void poner(boolean v) { c.ping = v; } });
            interruptor("Reloj", c.reloj, new Escritor() { public void poner(boolean v) { c.reloj = v; } });
            interruptor("Reloj de 24 horas", c.reloj24, new Escritor() { public void poner(boolean v) { c.reloj24 = v; } });
            interruptor("Velocidad", c.velocidad, new Escritor() { public void poner(boolean v) { c.velocidad = v; } });
            interruptor("Combo", c.combo, new Escritor() { public void poner(boolean v) { c.combo = v; } });
            interruptor("Alcance", c.alcance, new Escritor() { public void poner(boolean v) { c.alcance = v; } });
            interruptor("Sprint / Sneak", c.estado, new Escritor() { public void poner(boolean v) { c.estado = v; } });
            interruptor("Pociones", c.pociones, new Escritor() { public void poner(boolean v) { c.pociones = v; } });
            interruptor("Armadura", c.armadura, new Escritor() { public void poner(boolean v) { c.armadura = v; } });
            interruptor("Fondo oscuro", c.hudFondo, new Escritor() { public void poner(boolean v) { c.hudFondo = v; } });
            interruptor("Sombra del texto", c.hudSombra, new Escritor() { public void poner(boolean v) { c.hudSombra = v; } });
            color("Color del texto", true);
        } else if (pagina == 2) {
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
        } else if (pagina == 3) {
            ayuda = "Hora fija y despejado solo cambian lo que ves tú.";
            interruptor("Toggle sprint", c.toggleSprint, new Escritor() { public void poner(boolean v) { c.toggleSprint = v; } });
            interruptor("Toggle sneak", c.toggleSneak, new Escritor() { public void poner(boolean v) { c.toggleSneak = v; } });
            interruptor("Zoom (tecla C)", c.zoom, new Escritor() { public void poner(boolean v) { c.zoom = v; } });
            deslizador("Zoom: x", "", 1.5, 15, c.zoomFactor, new Numero() {
                public void poner(double v) { c.zoomFactor = (float) (Math.round(v * 2) / 2.0); } }, true);
            interruptor("Zoom suave", c.zoomSuave, new Escritor() { public void poner(boolean v) { c.zoomSuave = v; } });
            interruptor("Fullbright", c.fullbright, new Escritor() { public void poner(boolean v) { c.fullbright = v; } });
            interruptor("Ocultar fuego", c.ocultarFuego, new Escritor() { public void poner(boolean v) { c.ocultarFuego = v; } });
            interruptor("Hora fija", c.tiempoFijo, new Escritor() { public void poner(boolean v) { c.tiempoFijo = v; } });
            horaSlider(c);
            interruptor("Siempre despejado", c.despejado, new Escritor() { public void poner(boolean v) { c.despejado = v; } });
        } else if (pagina == 5) {
            ayuda = "Se instalan o quitan la próxima vez que abras el perfil PvP desde NexLauncher.";
            int k = 0;
            for (final String[] m : Catalogo.MODS) {
                final GuiButton[] ref = new GuiButton[1];
                ref[0] = boton(width / 2 - 156 + (k % 3) * 105, 56 + (k / 3) * 22, 102,
                        textoMod(m[1], Catalogo.elegido(c, m[0])), new Runnable() {
                            public void run() {
                                boolean v = !Catalogo.elegido(c, m[0]);
                                c.ponerExtra(Catalogo.VERSION, m[0], v, Catalogo.porDefecto());
                                ref[0].displayString = textoMod(m[1], v);
                            }
                        });
                k++;
            }
        } else {
            ayuda = "«Siempre crítico» y «Siempre filo» son solo visuales: no cambian el daño.";
            interruptor("Multiplicar partículas", c.particulas, new Escritor() { public void poner(boolean v) { c.particulas = v; } });
            deslizador("Multiplicador: x", "", 1, 10, c.multiplicador, new Numero() {
                public void poner(double v) { c.multiplicador = (int) Math.round(v); } });
            interruptor("Siempre crítico", c.siempreCritico, new Escritor() { public void poner(boolean v) { c.siempreCritico = v; } });
            interruptor("Siempre filo", c.siempreFilo, new Escritor() { public void poner(boolean v) { c.siempreFilo = v; } });
            interruptor("Contorno de bloque", c.contorno, new Escritor() { public void poner(boolean v) { c.contorno = v; } });
            color("Color del contorno", false);
            deslizador("Grosor: ", "", 1, 8, c.contornoGrosor, new Numero() {
                public void poner(double v) { c.contornoGrosor = (float) (Math.round(v * 2) / 2.0); } }, true);
        }
        boton(cx - 100, height - 26, 200, "Listo", new Runnable() { public void run() { cerrar(); } });
    }

    private static String textoMod(String nombre, boolean activo) {
        return (activo ? "§a+ §r" : "§8- §7") + nombre;
    }

    private int x() {
        return width / 2 + (fila % 2 == 0 ? -155 : 5);
    }

    private int y() {
        int y = 56 + (fila / 2) * 22;
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

    private void color(final String texto, final boolean deTexto) {
        final Config c = Config.get();
        final GuiButton[] ref = new GuiButton[1];
        int actual = deTexto ? c.hudColor : c.contornoColor;
        ref[0] = boton(x(), y(), 150, texto + ": " + Config.NOMBRES_COLOR[actual], new Runnable() {
            public void run() {
                int i = ((deTexto ? c.hudColor : c.contornoColor) + 1) % Config.COLORES.length;
                if (deTexto) {
                    c.hudColor = i;
                } else {
                    c.contornoColor = i;
                }
                ref[0].displayString = texto + ": " + Config.NOMBRES_COLOR[i];
            }
        });
    }

    private void horaSlider(final Config c) {
        final int id = acciones.size();
        acciones.add(null);
        final GuiSlider[] ref = new GuiSlider[1];
        ref[0] = new GuiSlider(id, x(), y(), 150, 20, "Hora: ", "", 0, 23999, c.hora, false, false,
                new GuiSlider.ISlider() {
                    public void onChangeSliderValue(GuiSlider s) {
                        c.hora = (int) (Math.round(s.getValue() / 250.0) * 250);
                        s.displayString = "Hora: " + Config.textoHora(c.hora);
                    }
                });
        ref[0].displayString = "Hora: " + Config.textoHora(c.hora);
        buttonList.add(ref[0]);
    }

    private void deslizador(String prefijo, String sufijo, double min, double max, double valor, Numero poner) {
        deslizador(prefijo, sufijo, min, max, valor, poner, false);
    }

    private void deslizador(String prefijo, String sufijo, double min, double max, double valor, final Numero poner,
                            boolean decimales) {
        int id = acciones.size();
        acciones.add(null);
        GuiSlider s = new GuiSlider(id, x(), y(), 150, 20, prefijo, sufijo, min, max, valor, decimales, true,
                new GuiSlider.ISlider() {
                    public void onChangeSliderValue(GuiSlider slider) {
                        poner.poner(slider.getValue());
                    }
                });
        if (decimales) {
            s.precision = 1;
            s.updateSlider();
        }
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
        drawCenteredString(fontRendererObj, "§lNexClient", width / 2, 10, 0xFFFFFF);
        drawCenteredString(fontRendererObj, ayuda, width / 2, height - 38, 0xAAAAAA);
        super.drawScreen(mx, my, parcial);
    }
}
