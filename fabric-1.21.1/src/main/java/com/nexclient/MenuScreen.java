package com.nexclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Menú de NexClient (Shift derecho). */
public class MenuScreen extends Screen {
    private static final String[] PAGINAS = {"HUD", "Info", "Blockhit", "Juego", "Visual", "Mira", "Mods"};
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
        int ancho = 46;
        int x0 = cx - (PAGINAS.length * (ancho + 2)) / 2;
        for (int i = 0; i < PAGINAS.length; i++) {
            final int k = i;
            Button b = Button.builder(Component.literal(i == pagina ? "§l" + PAGINAS[i] : PAGINAS[i]), x -> {
                pagina = k;
                rebuildWidgets();
            }).bounds(x0 + i * (ancho + 2), 28, ancho, 20).build();
            b.active = i != pagina;
            addRenderableWidget(b);
        }
        fila = 0;
        switch (pagina) {
            case 0:
                addRenderableWidget(Button.builder(Component.literal("§lEditar HUD§r · posición, tamaño, fondo, borde y colores"),
                        b -> minecraft.setScreen(new EditorHudScreen(this))).bounds(width / 2 - 155, y(), 310, 20).build());
                fila++;
                interruptor("Teclas en pantalla", c.teclas, v -> c.teclas = v, "WASD, clics y espacio en pantalla.");
                interruptor("CPS en las teclas", c.teclasCps, v -> c.teclasCps = v, null);
                interruptor("Botones del ratón", c.teclasRaton, v -> c.teclasRaton = v, null);
                interruptor("Barra espaciadora", c.teclasEspacio, v -> c.teclasEspacio = v, null);
                interruptor("Contador de CPS", c.cps, v -> c.cps = v, null);
                interruptor("Contador de FPS", c.fps, v -> c.fps = v, null);
                break;
            case 1:
                interruptor("Coordenadas", c.coords, v -> c.coords = v, null);
                interruptor("Dirección (N, S…)", c.coordsDireccion, v -> c.coordsDireccion = v, null);
                interruptor("Ping", c.ping, v -> c.ping = v, null);
                interruptor("Reloj", c.reloj, v -> c.reloj = v, null);
                interruptor("Reloj de 24 horas", c.reloj24, v -> c.reloj24 = v, null);
                interruptor("Velocidad", c.velocidad, v -> c.velocidad = v, "Bloques por segundo.");
                interruptor("Combo", c.combo, v -> c.combo = v, "Golpes seguidos sin que te peguen.");
                interruptor("Alcance", c.alcance, v -> c.alcance = v, "Distancia de tu último golpe.");
                interruptor("Sprint / Sneak", c.estado, v -> c.estado = v, null);
                interruptor("Pociones", c.pociones, v -> c.pociones = v, null);
                interruptor("Armadura", c.armadura, v -> c.armadura = v, "Durabilidad de armadura y arma.");
                interruptor("Ping en el Tab", c.pingTab, v -> c.pingTab = v,
                        "En la lista de jugadores (Tab) ves el ping de cada uno en números.");
                break;
            case 2:
                interruptor("Blockhit visual", c.blockhit, v -> c.blockhit = v,
                        "Con espada y clic derecho, la espada se pone en pose de bloqueo como en 1.8.");
                interruptor("Estilo 1.7", c.estilo17, v -> c.estilo17 = v,
                        "La espada sigue golpeando mientras bloqueas (animación de 1.7).");
                interruptor("Mantener = spam", c.blockhitMantener, v -> c.blockhitMantener = v,
                        "Mantener el clic izquierdo mientras bloqueas repite el golpe como si spamearas.");
                interruptor("Blockhit con comida", c.blockhitComida, v -> c.blockhitComida = v,
                        "Al comer o beber, el brazo sigue golpeando como en 1.7 (sin sonido).");
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
                break;
            case 3:
                interruptor("Toggle sprint", c.toggleSprint, v -> c.toggleSprint = v,
                        "Tu tecla de correr alterna: pulsa una vez y sigues corriendo.");
                interruptor("Toggle sneak", c.toggleSneak, v -> c.toggleSneak = v,
                        "Tu tecla de agacharse alterna.");
                interruptor("Zoom (tecla C)", c.zoom, v -> c.zoom = v, "Cámbiala en Opciones → Controles.");
                deslizador("Aumento del zoom", 1.5, 15, 0.5, c.zoomFactor, v -> c.zoomFactor = (float) v,
                        v -> String.format("x%.1f", v));
                interruptor("Zoom suave", c.zoomSuave, v -> c.zoomSuave = v, "Cámara cinemática al hacer zoom.");
                interruptor("Fullbright", c.fullbright, v -> c.fullbright = v, "Ves todo iluminado, también cuevas.");
                interruptor("Ocultar fuego", c.ocultarFuego, v -> c.ocultarFuego = v,
                        "Quita las llamas que tapan la pantalla.");
                interruptor("Hora fija", c.tiempoFijo, v -> c.tiempoFijo = v, "Solo la ves tú.");
                deslizador("Hora", 0, 23999, 250, c.hora, v -> c.hora = (int) Math.round(v),
                        v -> Config.textoHora((int) Math.round(v)));
                interruptor("Siempre despejado", c.despejado, v -> c.despejado = v, "Sin lluvia en tu pantalla.");
                addRenderableWidget(CycleButton.<Integer>builder(i -> Component.literal(Config.MODOS_VISTA[i]))
                        .withValues(java.util.Arrays.asList(0, 1, 2)).withInitialValue(c.vistaLibre)
                        .withTooltip(i -> Tooltip.create(Component.literal("Cámara libre en tercera persona: mueves el "
                                + "ratón y miras alrededor sin girar a tu personaje. Tecla en Opciones → Controles.")))
                        .create(x(), y(), 150, 20, Component.literal("Vista libre"), (b, v) -> c.vistaLibre = v));
                break;
            case 5:
                interruptor("Mira personalizada", c.mira, v -> c.mira = v, "Reemplaza la mira del juego.");
                addRenderableWidget(CycleButton.<Integer>builder(i -> Component.literal(Config.ESTILOS_MIRA[i]))
                        .withValues(java.util.Arrays.asList(0, 1, 2, 3, 4)).withInitialValue(c.miraEstilo)
                        .create(x(), y(), 150, 20, Component.literal("Estilo"), (b, v) -> c.miraEstilo = v));
                deslizador("Tamaño", 1, 20, 1, c.miraTamano, v -> c.miraTamano = (int) Math.round(v), v -> String.valueOf(Math.round(v)));
                deslizador("Grosor", 1, 6, 1, c.miraGrosor, v -> c.miraGrosor = (int) Math.round(v), v -> String.valueOf(Math.round(v)));
                deslizador("Separación", 0, 12, 1, c.miraHueco, v -> c.miraHueco = (int) Math.round(v), v -> String.valueOf(Math.round(v)));
                color("Color de la mira", c.miraColor, v -> c.miraColor = v);
                interruptor("Borde negro", c.miraBorde, v -> c.miraBorde = v, null);
                fila++;
                interruptor("Color de golpe", c.colorGolpe, v -> c.colorGolpe = v,
                        "Cambia el rojo con el que parpadean los jugadores al recibir daño.");
                color("Color", c.colorGolpeIdx, v -> c.colorGolpeIdx = v);
                break;
            case 6: {
                int k = 0;
                for (String[] m : Catalogo.MODS) {
                    final String id = m[0], nombre = m[1];
                    int bx = width / 2 - 156 + (k % 3) * 105, by = 56 + (k / 3) * 22;
                    Button b = Button.builder(textoMod(nombre, Catalogo.elegido(c, id)), x -> {
                        boolean v = !Catalogo.elegido(c, id);
                        c.ponerExtra(Catalogo.VERSION, id, v, Catalogo.porDefecto());
                        x.setMessage(textoMod(nombre, v));
                    }).bounds(bx, by, 102, 20).build();
                    b.setTooltip(Tooltip.create(Component.literal(m[2])));
                    addRenderableWidget(b);
                    k++;
                }
                break;
            }
            default:
                interruptor("Multiplicar partículas", c.particulas, v -> c.particulas = v,
                        "Más partículas de crítico y de filo al golpear.");
                deslizador("Multiplicador", 1, 10, 1, c.multiplicador, v -> c.multiplicador = (int) Math.round(v),
                        v -> "x" + Math.round(v));
                interruptor("Siempre crítico", c.siempreCritico, v -> c.siempreCritico = v, "Solo visual.");
                interruptor("Siempre filo", c.siempreFilo, v -> c.siempreFilo = v, "Solo visual.");
                interruptor("Contorno de bloque", c.contorno, v -> c.contorno = v, "Color propio al mirar un bloque.");
                color("Color del contorno", c.contornoColor, v -> c.contornoColor = v);
                break;
        }
        addRenderableWidget(Button.builder(Component.literal("Listo"), b -> onClose())
                .bounds(cx - 100, height - 26, 200, 20).build());
    }

    private static Component textoMod(String nombre, boolean activo) {
        return Component.literal((activo ? "§a✔ §r" : "§8✖ §7") + nombre);
    }

    private int x() {
        return width / 2 + (fila % 2 == 0 ? -155 : 5);
    }

    private int y() {
        int y = 56 + (fila / 2) * 22;
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

    private void color(String texto, int valor, Consumer<Integer> poner) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < Config.COLORES.length; i++) {
            indices.add(i);
        }
        int x = x();
        addRenderableWidget(CycleButton.<Integer>builder(i -> Component.literal(Config.NOMBRES_COLOR[i])
                        .withStyle(s -> s.withColor(Config.COLORES[i] & 0xFFFFFF)))
                .withValues(indices).withInitialValue(valor)
                .create(x, y(), 150, 20, Component.literal(texto), (b, v) -> poner.accept(v)));
    }

    private void deslizador(String texto, double min, double max, double paso, double valor, DoubleConsumer poner,
                            DoubleFunction<String> formato) {
        int x = x();
        addRenderableWidget(new Deslizador(x, y(), texto, min, max, paso, valor, poner, formato));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, "§lNexClient", width / 2, 10, 0xFFFFFF);
        if (pagina == 5 && Config.get().mira) {
            Hud.mira(g, width / 2, height - 52);
        }
        if (pagina == 6) {
            g.drawCenteredString(font, "Se instalan o quitan la próxima vez que abras el perfil PvP desde NexLauncher.",
                    width / 2, height - 38, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        Config.get().guardar();
        minecraft.setScreen(padre);
    }

    private static final class Deslizador extends AbstractSliderButton {
        private final String texto;
        private final double min, max, paso;
        private final DoubleConsumer poner;
        private final DoubleFunction<String> formato;

        Deslizador(int x, int y, String texto, double min, double max, double paso, double valor,
                   DoubleConsumer poner, DoubleFunction<String> formato) {
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
