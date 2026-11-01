package com.nexclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Editor de la mira: vista previa grande, 13 formas, tamaño, grosor, separación, opacidad, colores, borde, mira
 * dinámica, color al apuntar, destello, 5 ranuras guardadas y un código para compartirla. En «Propia» la vista
 * previa es una cuadrícula para pintarla píxel por píxel (clic: pintar, clic derecho: borrar).
 */
public class EditorMiraScreen extends Screen {
    /** Paleta de colores rápida (RGB). */
    private static final int[] PALETA = {0xFFFFFF, 0xAAAAAA, 0x000000, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x00AA00,
            0x55FFFF, 0x5555FF, 0xFF55FF, 0xAA00AA};
    private static final int CELDA = 7;
    private final Screen padre;
    private int vx, vy, vt;               // vista previa: esquina y lado
    private int colA, colB, fila0;
    private int pintando = -1;            // 1 pinta, 0 borra (al arrastrar sobre la cuadrícula)
    private String aviso = "";
    private long avisoHasta;

    public EditorMiraScreen(Screen padre) {
        super(Component.literal("Mira"));
        this.padre = padre;
    }

    @Override
    protected void init() {
        Config c = Config.get();
        c.mira = true;
        vt = Miras.LADO * CELDA + 4;
        int total = vt + 10 + 150 + 4 + 150;
        vx = Math.max(4, width / 2 - total / 2);
        vy = 40;
        colA = vx + vt + 10;
        colB = colA + 154;
        fila0 = 40;
        // columna A: forma y color
        int y = fila0;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> estilo(-1)).bounds(colA, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(Miras.ESTILOS[c.miraEstilo]), b -> estilo(1))
                .bounds(colA + 22, y, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> estilo(1)).bounds(colA + 130, y, 20, 20).build());
        y += 22;
        addRenderableWidget(new Deslizador(colA, y, "Tamaño", 1, 20, c.miraTamano, v -> c.miraTamano = (int) v));
        y += 22;
        addRenderableWidget(new Deslizador(colA, y, "Grosor", 1, 6, c.miraGrosor, v -> c.miraGrosor = (int) v));
        y += 22;
        addRenderableWidget(new Deslizador(colA, y, "Separación", 0, 12, c.miraHueco, v -> c.miraHueco = (int) v));
        y += 22;
        addRenderableWidget(new Deslizador(colA, y, "Opacidad %", 10, 100, c.miraOpacidad, v -> c.miraOpacidad = (int) v));
        y = filaColor() + 12;  // antes, la fila de la paleta del color (se dibuja en render)
        interruptor(colA, y, "Borde", c.miraBorde, v -> c.miraBorde = v, null);
        y += 22;
        addRenderableWidget(new Deslizador(colA, y, "Grosor del borde", 1, 3, c.miraBordeGrosor,
                v -> c.miraBordeGrosor = (int) v));
        // columna B: efectos, ranuras y código
        y = fila0;
        interruptor(colB, y, "Arcoíris", c.miraArcoiris, v -> c.miraArcoiris = v, "El color va cambiando solo.");
        y += 22;
        interruptor(colB, y, "Dinámica", c.miraDinamica, v -> c.miraDinamica = v,
                "Se abre al correr o saltar y vuelve suave.");
        y += 22;
        interruptor(colB, y, "Color al apuntar", c.miraObjetivo, v -> c.miraObjetivo = v,
                "Cambia de color cuando apuntas a un jugador (elige el color abajo).");
        y = filaObjetivo() + 12;
        interruptor(colB, y, "Destello al golpear", c.miraDestello, v -> c.miraDestello = v, null);
        y += 22;
        interruptor(colB, y, "En tercera persona", c.miraTercera, v -> c.miraTercera = v, null);
        y += 22;
        interruptor(colB, y, "Ocultar con arco", c.miraOcultarArco, v -> c.miraOcultarArco = v,
                "No se ve mientras tensas el arco o la ballesta.");
        y = filaBorde();
        addRenderableWidget(Button.builder(Component.literal("Copiar código"), b -> {
            minecraft.keyboardHandler.setClipboard(Miras.codigo(Config.get()));
            avisar("Código copiado: pégalo a un amigo o en NexLauncher.");
        }).bounds(colB, y, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Pegar código"), b -> {
            if (Miras.aplicar(Config.get(), minecraft.keyboardHandler.getClipboard())) {
                avisar("Mira cargada.");
                rebuildWidgets();
            } else {
                avisar("Lo copiado no es un código de mira (empieza con NXM:).");
            }
        }).bounds(colB + 76, y, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Listo"), b -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());
    }

    private void interruptor(int x, int y, String texto, boolean valor, Consumer<Boolean> poner, String ayuda) {
        CycleButton<Boolean> b = CycleButton.onOffBuilder(valor).create(x, y, 150, 20, Component.literal(texto),
                (boton, v) -> poner.accept(v));
        if (ayuda != null) {
            b.setTooltip(Tooltip.create(Component.literal(ayuda)));
        }
        addRenderableWidget(b);
    }

    private void estilo(int paso) {
        Config c = Config.get();
        c.miraEstilo = Math.floorMod(c.miraEstilo + paso, Miras.ESTILOS.length);
        rebuildWidgets();
    }

    private void avisar(String texto) {
        aviso = texto;
        avisoHasta = System.currentTimeMillis() + 3500;
    }

    // ---- paletas y ranuras (dibujadas a mano)
    private int filaColor() {
        return fila0 + 110;
    }

    private int filaObjetivo() {
        return fila0 + 66;
    }

    /** Paleta del borde (columna A) y botones del código (columna B). */
    private int filaBorde() {
        return fila0 + 166;
    }

    private int filaRanuras() {
        return fila0 + 146;
    }

    private void paleta(GuiGraphics g, int x, int y, int actual, String titulo, int ancho) {
        g.drawString(font, titulo, x, y + 2, 0xFFAAAAAA, false);
        int x0 = x + font.width(titulo) + 4, lado = Math.max(6, Math.min(10, (ancho - font.width(titulo) - 4) / PALETA.length - 1));
        for (int i = 0; i < PALETA.length; i++) {
            int px = x0 + i * (lado + 1);
            g.fill(px, y + 1, px + lado, y + 1 + lado, 0xFF000000 | PALETA[i]);
            if (PALETA[i] == (actual & 0xFFFFFF)) {
                g.renderOutline(px - 1, y, lado + 2, lado + 2, 0xFFFFFF55);
            }
        }
    }

    private int colorEn(double mx, double my, int x, int y, String titulo, int ancho) {
        int x0 = x + font.width(titulo) + 4, lado = Math.max(6, Math.min(10, (ancho - font.width(titulo) - 4) / PALETA.length - 1));
        if (my < y || my > y + lado + 1) {
            return -1;
        }
        int i = (int) ((mx - x0) / (lado + 1));
        return mx >= x0 && i >= 0 && i < PALETA.length ? PALETA[i] : -1;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        Config c = Config.get();
        g.drawCenteredString(font, "§lMira", width / 2, 10, 0xFFFFFF);
        g.drawCenteredString(font, "Así se ve en tu pantalla (vista previa al lado, más grande)", width / 2, 22, 0xFF888888);
        // vista previa
        g.fill(vx, vy, vx + vt, vy + vt, 0xFF3A5A2A);
        g.fill(vx, vy + vt / 2, vx + vt, vy + vt, 0xFF6B4A2B);
        if (c.miraEstilo == Miras.PROPIA) {  // cuadrícula para pintar
            for (int i = 0; i < Miras.LADO * Miras.LADO; i++) {
                int x = vx + 2 + (i % Miras.LADO) * CELDA, y = vy + 2 + (i / Miras.LADO) * CELDA;
                boolean on = c.miraPixeles.charAt(i) == '1';
                g.fill(x, y, x + CELDA - 1, y + CELDA - 1, on ? (0xFF000000 | c.miraRGB) : (i == Miras.LADO * Miras.LADO / 2 ? 0x55FF55FF : 0x33000000));
            }
        } else {
            g.pose().pushPose();
            g.pose().translate(vx + vt / 2f, vy + vt / 2f, 0);
            g.pose().scale(4f, 4f, 1f);
            Hud.mira(g, 0, 0);
            g.pose().popPose();
        }
        // tamaño real, debajo
        g.fill(vx, vy + vt + 6, vx + vt, vy + vt + 46, 0xFF3A5A2A);
        Hud.mira(g, vx + vt / 2, vy + vt + 26);
        g.drawString(font, "Tamaño real", vx, vy + vt + 49, 0xFF888888, false);
        paleta(g, colA, filaColor(), c.miraRGB, "Color", 150);
        paleta(g, colB, filaObjetivo(), c.miraObjetivoRGB, "Al apuntar", 150);
        paleta(g, colA, filaBorde(), c.miraBordeRGB, "Borde", 150);
        // ranuras
        int ry = filaRanuras();
        g.drawString(font, "Ranuras:", colB, ry + 4, 0xFFAAAAAA, false);
        for (int i = 0; i < 5; i++) {
            int x = colB + 46 + i * 21;
            boolean llena = i < c.miraRanuras.size() && c.miraRanuras.get(i) != null && !c.miraRanuras.get(i).isEmpty();
            boolean encima = mx >= x && mx < x + 19 && my >= ry && my < ry + 16;
            g.fill(x, ry, x + 19, ry + 16, encima ? 0xFF555555 : 0xFF333333);
            g.drawCenteredString(font, (llena ? "§a" : "§7") + (i + 1), x + 10, ry + 4, 0xFFFFFF);
        }
        if (mx >= colB && my >= ry && my < ry + 16 && mx < colB + 46 + 5 * 21) {
            g.renderTooltip(font, List.of(Component.literal("Clic: cargar"), Component.literal("Clic derecho: guardar aquí")),
                    java.util.Optional.empty(), mx, my);
        }
        if (System.currentTimeMillis() < avisoHasta) {
            g.drawCenteredString(font, aviso, width / 2, height - 40, 0xFF55FF55);
        } else if (c.miraEstilo == Miras.PROPIA) {
            g.drawCenteredString(font, "Clic: pintar · Clic derecho: borrar (el centro está marcado)", width / 2,
                    height - 40, 0xFF888888);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        if (super.mouseClicked(mx, my, boton)) {
            return true;
        }
        Config c = Config.get();
        int k;
        if ((k = colorEn(mx, my, colA, filaColor(), "Color", 150)) >= 0) {
            c.miraRGB = k;
            c.miraArcoiris = false;
            rebuildWidgets();
            return true;
        }
        if ((k = colorEn(mx, my, colB, filaObjetivo(), "Al apuntar", 150)) >= 0) {
            c.miraObjetivoRGB = k;
            return true;
        }
        if ((k = colorEn(mx, my, colA, filaBorde(), "Borde", 150)) >= 0) {
            c.miraBordeRGB = k;
            return true;
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
                    rebuildWidgets();
                } else {
                    avisar("La ranura " + (i + 1) + " está vacía: clic derecho para guardar ahí.");
                }
                return true;
            }
        }
        if (c.miraEstilo == Miras.PROPIA && pintar(mx, my, boton == 0 ? 1 : 0)) {
            pintando = boton == 0 ? 1 : 0;
            return true;
        }
        return false;
    }

    private boolean pintar(double mx, double my, int valor) {
        int cx = (int) ((mx - vx - 2) / CELDA), cy = (int) ((my - vy - 2) / CELDA);
        if (mx < vx + 2 || my < vy + 2 || cx >= Miras.LADO || cy >= Miras.LADO) {
            return false;
        }
        Config c = Config.get();
        int i = cy * Miras.LADO + cx;
        char[] p = c.miraPixeles.toCharArray();
        p[i] = valor == 1 ? '1' : '0';
        c.miraPixeles = new String(p);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int boton, double dx, double dy) {
        if (pintando >= 0) {
            pintar(mx, my, pintando);
            return true;
        }
        return super.mouseDragged(mx, my, boton, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int boton) {
        pintando = -1;
        return super.mouseReleased(mx, my, boton);
    }

    @Override
    public void onClose() {
        Config.get().guardar();
        minecraft.setScreen(padre);
    }

    private static final class Deslizador extends AbstractSliderButton {
        private final String texto;
        private final double min, max;
        private final DoubleConsumer poner;

        Deslizador(int x, int y, String texto, double min, double max, double valor, DoubleConsumer poner) {
            super(x, y, 150, 20, Component.empty(), (valor - min) / (max - min));
            this.texto = texto;
            this.min = min;
            this.max = max;
            this.poner = poner;
            updateMessage();
        }

        private double real() {
            return Math.round(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(texto + ": " + (int) real()));
        }

        @Override
        protected void applyValue() {
            poner.accept(real());
        }
    }
}
