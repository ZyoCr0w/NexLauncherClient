package com.nexclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/** Menú de NexClient (Shift derecho): HUD, Blockhit y Partículas. */
public class MenuScreen extends Screen {
    private static final String[] PAGINAS = {"HUD", "Blockhit", "Partículas"};
    private static int pagina = 0;
    private final Screen padre;
    private int fila;

    public MenuScreen(Screen padre) {
        super(Component.literal("NexClient"));
        this.padre = padre;
    }

    @Override
    protected void init() {
        Config c = Config.get();
        int cx = width / 2;
        for (int i = 0; i < PAGINAS.length; i++) {
            final int k = i;
            Button b = Button.builder(Component.literal(i == pagina ? "§l" + PAGINAS[i] : PAGINAS[i]), x -> {
                pagina = k;
                rebuildWidgets();
            }).bounds(cx - 155 + i * 105, 32, 100, 20).build();
            b.active = i != pagina;
            addRenderableWidget(b);
        }
        fila = 0;
        if (pagina == 0) {
            interruptor("Teclas en pantalla", c.teclas, v -> c.teclas = v, "WASD, clics y espacio en pantalla.");
            interruptor("CPS en las teclas", c.teclasCps, v -> c.teclasCps = v, null);
            interruptor("Botones del ratón", c.teclasRaton, v -> c.teclasRaton = v, null);
            interruptor("Barra espaciadora", c.teclasEspacio, v -> c.teclasEspacio = v, null);
            interruptor("Contador de CPS", c.cps, v -> c.cps = v, "Clics por segundo: izquierdo | derecho.");
            interruptor("Contador de FPS", c.fps, v -> c.fps = v, null);
            deslizador("Tamaño de las teclas", 0.5, 2.5, 0.05, c.teclasEscala, v -> c.teclasEscala = (float) v,
                    v -> Math.round(v * 100) + "%");
            addRenderableWidget(Button.builder(Component.literal("Mover elementos del HUD…"),
                    b -> minecraft.setScreen(new EditorHudScreen(this))).bounds(cx + 5, y(), 150, 20).build());
        } else if (pagina == 1) {
            interruptor("Blockhit visual", c.blockhit, v -> c.blockhit = v,
                    "Con espada y clic derecho, la espada se pone en pose de bloqueo como en 1.8.");
            interruptor("Estilo 1.7", c.estilo17, v -> c.estilo17 = v,
                    "La espada sigue golpeando mientras bloqueas (animación de 1.7).");
            interruptor("Ocultar escudo", c.ocultarEscudo, v -> c.ocultarEscudo = v,
                    "Si bloqueas con un escudo en la otra mano, no se dibuja.");
            interruptor("Sonido de blockhit", c.sonido, v -> c.sonido = v, null);
            addRenderableWidget(CycleButton.<String>builder(id -> Component.literal(Config.nombreSonido(id)))
                    .withValues(Arrays.asList(Config.SONIDOS)).withInitialValue(c.sonidoId)
                    .create(x(), y(), 150, 20, Component.literal("Sonido"), (b, v) -> {
                        c.sonidoId = v;
                        Efectos.sonar();
                    }));
            interruptor("Sonar en cada golpe", c.sonidoCadaGolpe, v -> c.sonidoCadaGolpe = v,
                    "Apagado: solo suena al hacer blockhit.");
            deslizador("Volumen", 0, 2, 0.05, c.volumen, v -> c.volumen = (float) v, v -> Math.round(v * 100) + "%");
            deslizador("Tono", 0.5, 2, 0.05, c.tono, v -> c.tono = (float) v, v -> String.format("%.2f", v));
            addRenderableWidget(Button.builder(Component.literal("Probar sonido"), b -> Efectos.sonar())
                    .bounds(x(), y(), 150, 20).build());
        } else {
            interruptor("Multiplicar partículas", c.particulas, v -> c.particulas = v,
                    "Más partículas de crítico y de filo al golpear.");
            deslizador("Multiplicador", 1, 10, 1, c.multiplicador, v -> c.multiplicador = (int) Math.round(v),
                    v -> "x" + Math.round(v));
            interruptor("Siempre crítico", c.siempreCritico, v -> c.siempreCritico = v,
                    "Muestra partículas de crítico en todos los golpes (solo visual).");
            interruptor("Siempre filo", c.siempreFilo, v -> c.siempreFilo = v,
                    "Partículas de filo aunque la espada no esté encantada (solo visual).");
        }
        addRenderableWidget(Button.builder(Component.literal("Listo"), b -> onClose())
                .bounds(cx - 100, height - 28, 200, 20).build());
    }

    private int x() {
        return width / 2 + (fila % 2 == 0 ? -155 : 5);
    }

    private int y() {
        int y = 64 + (fila / 2) * 24;
        fila++;
        return y;
    }

    private void interruptor(String texto, boolean valor, Consumer<Boolean> poner, String ayuda) {
        int x = x();
        CycleButton<Boolean> b = CycleButton.onOffBuilder(valor).create(x, y(), 150, 20, Component.literal(texto),
                (boton, v) -> poner.accept(v));
        if (ayuda != null) {
            b.setTooltip(Tooltip.create(Component.literal(ayuda)));
        }
        addRenderableWidget(b);
    }

    private void deslizador(String texto, double min, double max, double paso, double valor, Consumer<Double> poner,
                            DoubleFunction<String> formato) {
        int x = x();
        addRenderableWidget(new Deslizador(x, y(), texto, min, max, paso, valor, poner, formato));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, "§lNexClient", width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        Config.get().guardar();
        minecraft.setScreen(padre);
    }

    private static final class Deslizador extends AbstractSliderButton {
        private final String texto;
        private final double min, max, paso;
        private final Consumer<Double> poner;
        private final DoubleFunction<String> formato;

        Deslizador(int x, int y, String texto, double min, double max, double paso, double valor,
                   Consumer<Double> poner, DoubleFunction<String> formato) {
            super(x, y, 150, 20, Component.empty(), (valor - min) / (max - min));
            this.texto = texto;
            this.min = min;
            this.max = max;
            this.paso = paso;
            this.poner = poner;
            this.formato = formato;
            updateMessage();
        }

        private double real() {
            double v = min + value * (max - min);
            return Math.round(v / paso) * paso;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(texto + ": " + formato.apply(real())));
        }

        @Override
        protected void applyValue() {
            poner.accept(real());
        }
    }
}
