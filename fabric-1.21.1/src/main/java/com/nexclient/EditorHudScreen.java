package com.nexclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Editor del HUD estilo Lunar: arrastra cada elemento, la rueda cambia su tamaño y al hacer clic aparece su
 * panel con escala, fondo, borde, colores y sombra.
 */
public class EditorHudScreen extends Screen {
    private final Screen padre;
    private String arrastrando, elegido;
    private double dx, dy;
    private boolean ocultos = true;

    public EditorHudScreen(Screen padre) {
        super(Component.literal("Editar HUD"));
        this.padre = padre;
    }

    @Override
    protected void init() {
        Config c = Config.get();
        addRenderableWidget(Button.builder(Component.literal("Listo"), b -> onClose())
                .bounds(width / 2 - 75, height - 24, 70, 20).build());
        addRenderableWidget(CycleButton.onOffBuilder(ocultos).create(width / 2 + 5, height - 24, 140, 20,
                Component.literal("Ver ocultos"), (b, v) -> ocultos = v));
        if (elegido == null) {
            return;
        }
        Config.Estilo e = c.estilo(elegido);
        int[] r = Hud.rect(elegido, minecraft);
        int x = r[0] + r[2] / 2 < width / 2 ? width - 156 : 6;
        int y = 24;
        addRenderableWidget(CycleButton.onOffBuilder(Hud.activo(c, elegido)).create(x, y, 150, 20,
                Component.literal("Mostrar"), (b, v) -> Hud.poner(c, elegido, v)));
        y += 22;
        addRenderableWidget(new Deslizador(x, y, "Escala", 0.5, 3, 0.05, e.escala, v -> e.escala = (float) v,
                v -> Math.round(v * 100) + "%"));
        y += 22;
        addRenderableWidget(new Deslizador(x, y, "Fondo", 0, 100, 5, e.fondo, v -> e.fondo = (int) Math.round(v),
                v -> Math.round(v) + "%"));
        y += 22;
        addRenderableWidget(color(x, y, "Texto", e.colorTexto, v -> e.colorTexto = v));
        y += 22;
        addRenderableWidget(CycleButton.onOffBuilder(e.arcoiris).create(x, y, 150, 20,
                Component.literal("Arcoíris"), (b, v) -> e.arcoiris = v));
        y += 22;
        addRenderableWidget(CycleButton.onOffBuilder(e.sombra).create(x, y, 150, 20, Component.literal("Sombra"),
                (b, v) -> e.sombra = v));
        y += 22;
        addRenderableWidget(CycleButton.onOffBuilder(e.borde).create(x, y, 150, 20, Component.literal("Borde"),
                (b, v) -> e.borde = v));
        y += 22;
        addRenderableWidget(color(x, y, "Color del borde", e.colorBorde, v -> e.colorBorde = v));
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Restablecer"), b -> {
            Hud.restablecer(elegido);
            rebuildWidgets();
        }).bounds(x, y, 150, 20).build());
    }

    private CycleButton<Integer> color(int x, int y, String texto, int valor, java.util.function.IntConsumer poner) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < Config.COLORES.length; i++) {
            indices.add(i);
        }
        return CycleButton.<Integer>builder(i -> Component.literal(Config.NOMBRES_COLOR[i])
                        .withStyle(s -> s.withColor(Config.COLORES[i] & 0xFFFFFF)))
                .withValues(indices).withInitialValue(valor)
                .create(x, y, 150, 20, Component.literal(texto), (b, v) -> poner.accept(v));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x55000000);  // sin desenfoque: se ve el juego detrás
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        renderBackground(g, mx, my, delta);
        Config c = Config.get();
        for (String id : Hud.IDS) {
            boolean activo = Hud.activo(c, id);
            if (!activo && !ocultos) {
                continue;
            }
            int[] r = Hud.rect(id, minecraft);
            if (activo) {
                Hud.dibujarUno(g, minecraft, c, id, true, 0.016f);
            } else {
                g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x33FF5555);
                g.drawString(font, Hud.nombre(id), r[0] + 2, r[1] + 2, 0xFFFF9999, false);
            }
            boolean sel = id.equals(elegido);
            boolean encima = id.equals(arrastrando) || dentro(r, mx, my);
            g.renderOutline(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2,
                    sel ? 0xFFFFFF55 : encima ? 0xFFFFFFFF : 0x66FFFFFF);
            if (sel || encima) {
                g.drawString(font, Hud.nombre(id) + (activo ? "" : " (oculto)"), r[0], Math.max(0, r[1] - 10),
                        0xFFFFFFFF, true);
            }
        }
        for (GuiEventListener w : children()) {
            if (w instanceof Renderable) {
                ((Renderable) w).render(g, mx, my, delta);
            }
        }
        g.drawCenteredString(font, "Clic: elegir · Arrastra: mover · Rueda: tamaño", width / 2, 6, 0xFFFFFFFF);
    }

    private static boolean dentro(int[] r, double x, double y) {
        return x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3];
    }

    private String bajo(double mx, double my) {
        Config c = Config.get();
        for (int i = Hud.IDS.length - 1; i >= 0; i--) {  // el de encima primero
            String id = Hud.IDS[i];
            if ((Hud.activo(c, id) || ocultos) && dentro(Hud.rect(id, minecraft), mx, my)) {
                return id;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        if (super.mouseClicked(mx, my, boton)) {
            return true;
        }
        if (boton == 0) {
            String id = bajo(mx, my);
            if (id != null) {
                int[] r = Hud.rect(id, minecraft);
                arrastrando = id;
                dx = mx - r[0];
                dy = my - r[1];
            }
            if (id == null ? elegido != null : !id.equals(elegido)) {
                elegido = id;
                rebuildWidgets();
            }
            return id != null;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int boton, double ddx, double ddy) {
        if (arrastrando != null) {
            Hud.mover(arrastrando, (int) Math.round(mx - dx), (int) Math.round(my - dy));
            int[] r = Hud.rect(arrastrando, minecraft);  // ya ajustado a la pantalla
            Hud.mover(arrastrando, r[0], r[1]);
            return true;
        }
        return super.mouseDragged(mx, my, boton, ddx, ddy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int boton) {
        if (arrastrando != null) {
            arrastrando = null;
            rebuildWidgets();  // el panel se pasa al otro lado si hace falta
            return true;
        }
        return super.mouseReleased(mx, my, boton);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        String id = bajo(mx, my);
        if (id != null && sy != 0) {
            Config.Estilo e = Config.get().estilo(id);
            e.escala = Math.max(0.5f, Math.min(3f, Math.round((e.escala + (sy > 0 ? 0.1f : -0.1f)) * 20) / 20f));
            if (id.equals(elegido)) {
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
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
            return Math.round((min + value * (max - min)) / paso) * paso;
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
