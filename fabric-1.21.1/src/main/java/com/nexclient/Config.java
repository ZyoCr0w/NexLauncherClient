package com.nexclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ajustes de NexClient. El mismo archivo (.minecraft/config/nexclient.json) lo usan la versión 1.8.9,
 * la 1.21.1 y NexLauncher, así que se configura una sola vez.
 */
public class Config {
    // ---- HUD: teclas
    public boolean teclas = true;
    public int teclasX = 6, teclasY = 6;
    public float teclasEscala = 1.0f;
    public boolean teclasCps = true;
    public boolean teclasRaton = true;
    public boolean teclasEspacio = true;
    public boolean cps = false;
    public int cpsX = 6, cpsY = 100;
    public boolean fps = false;
    public int fpsX = 6, fpsY = 114;

    // ---- HUD: información
    public boolean coords = false;
    public boolean coordsDireccion = true;
    public boolean ping = false;
    public boolean reloj = false;
    public boolean reloj24 = true;
    public boolean velocidad = false;
    public boolean combo = false;
    public boolean alcance = false;
    public boolean estado = false;
    public boolean pociones = false;
    public boolean armadura = false;
    /** Posición de cada elemento nuevo: id -> {x, y}. Una x enorme lo pega al borde derecho. */
    public Map<String, int[]> pos = new HashMap<String, int[]>();
    public boolean hudFondo = true;
    public boolean hudSombra = true;
    public int hudColor = 0;
    /** Estilo propio de cada elemento del HUD (se crea con los valores generales de arriba). */
    public Map<String, Estilo> estilos = new HashMap<String, Estilo>();
    /** Mods extra elegidos en el juego, por versión ("1.8.9", "1.21.1"); NexLauncher los instala al jugar. */
    public Map<String, List<String>> extras = new HashMap<String, List<String>>();

    public static class Estilo {
        public float escala = 1.0f;
        /** Opacidad del fondo, 0–100 %. */
        public int fondo = 40;
        public boolean sombra = true;
        public int colorTexto = 0;
        public boolean borde = false;
        public int colorBorde = 0;
        /** Texto arcoíris (chroma): cada letra cambia de color con el tiempo. */
        public boolean arcoiris = false;
    }

    // ---- Blockhit
    public boolean blockhit = true;
    /** true: la espada se sigue moviendo al golpear mientras bloqueas (como en 1.7). */
    public boolean estilo17 = true;
    /** Animación de 1.7 también al comer o beber: el brazo sigue golpeando (sin sonido de blockhit). */
    public boolean blockhitComida = true;
    /** 1.21: al bloquear con espada no se dibuja el escudo de la otra mano. */
    public boolean ocultarEscudo = true;
    public boolean sonido = true;
    public String sonidoId = "metal";
    public float volumen = 1.0f;
    public float tono = 1.0f;
    /** false: solo suena al hacer blockhit; true: en cada golpe. */
    public boolean sonidoCadaGolpe = false;

    // ---- Partículas
    public boolean particulas = true;
    public int multiplicador = 2;
    public boolean siempreCritico = false;
    public boolean siempreFilo = false;

    // ---- Juego
    public boolean toggleSprint = false;
    public boolean toggleSneak = false;
    public boolean zoom = true;
    public float zoomFactor = 4.0f;
    public boolean zoomSuave = true;
    public boolean fullbright = false;
    public boolean ocultarFuego = false;
    public boolean tiempoFijo = false;
    public int hora = 6000;
    public boolean despejado = false;

    // ---- Visual
    public boolean contorno = false;
    public int contornoColor = 0;
    public float contornoGrosor = 2.0f;

    public static final String[] SONIDOS = {"metal", "escudo", "golpe", "orbe", "pling", "click"};
    public static final String[] NOMBRES = {"Metal", "Escudo", "Golpe", "Experiencia", "Pling", "Click"};
    public static final int[] COLORES = {0xFFFFFFFF, 0xFF55FFFF, 0xFF55FF55, 0xFFFFFF55, 0xFFFF5555, 0xFFFF55FF,
            0xFFFFAA00, 0xFF5555FF, 0xFF000000};
    public static final String[] NOMBRES_COLOR = {"Blanco", "Aqua", "Verde", "Amarillo", "Rojo", "Rosa", "Naranja",
            "Azul", "Negro"};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config actual = new Config();
    private static File archivo;
    private static long modificado;

    public static Config get() {
        return actual;
    }

    public static void cargar(File f) {
        archivo = f;
        Config c = null;
        if (f.isFile()) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                c = GSON.fromJson(r, Config.class);
            } catch (Exception e) {
                System.err.println("[NexClient] nexclient.json ilegible, se usan los valores por defecto: " + e);
            }
            modificado = f.lastModified();
        }
        actual = c != null ? c : new Config();
        actual.limitar();
    }

    /** Si NexLauncher cambió el archivo con el juego abierto, se vuelve a leer. */
    public static void recargarSiCambio() {
        if (archivo != null && archivo.isFile() && archivo.lastModified() != modificado) {
            cargar(archivo);
        }
    }

    public void guardar() {
        if (archivo == null) {
            return;
        }
        limitar();
        try {
            File carpeta = archivo.getParentFile();
            if (carpeta != null) {
                carpeta.mkdirs();
            }
            File tmp = new File(archivo.getPath() + ".tmp");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
            if (!tmp.renameTo(archivo)) {
                archivo.delete();
                tmp.renameTo(archivo);
            }
            modificado = archivo.lastModified();
        } catch (Exception e) {
            System.err.println("[NexClient] No se pudo guardar nexclient.json: " + e);
        }
    }

    private void limitar() {
        teclasEscala = Math.max(0.5f, Math.min(2.5f, teclasEscala));
        multiplicador = Math.max(1, Math.min(10, multiplicador));
        volumen = Math.max(0f, Math.min(2f, volumen));
        tono = Math.max(0.5f, Math.min(2f, tono));
        zoomFactor = Math.max(1.5f, Math.min(15f, zoomFactor));
        hora = Math.max(0, Math.min(23999, hora));
        contornoGrosor = Math.max(1f, Math.min(8f, contornoGrosor));
        hudColor = Math.max(0, Math.min(COLORES.length - 1, hudColor));
        contornoColor = Math.max(0, Math.min(COLORES.length - 1, contornoColor));
        if (pos == null) {
            pos = new HashMap<String, int[]>();
        }
        if (estilos == null) {
            estilos = new HashMap<String, Estilo>();
        }
        if (extras == null) {
            extras = new HashMap<String, List<String>>();
        }
        for (Estilo e : estilos.values()) {
            if (e != null) {
                e.escala = Math.max(0.5f, Math.min(3f, e.escala));
                e.fondo = Math.max(0, Math.min(100, e.fondo));
                e.colorTexto = Math.max(0, Math.min(COLORES.length - 1, e.colorTexto));
                e.colorBorde = Math.max(0, Math.min(COLORES.length - 1, e.colorBorde));
            }
        }
        if (indiceSonido(sonidoId) < 0) {
            sonidoId = "metal";
        }
    }

    public static int indiceSonido(String id) {
        for (int i = 0; i < SONIDOS.length; i++) {
            if (SONIDOS[i].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public static String nombreSonido(String id) {
        int i = indiceSonido(id);
        return i < 0 ? id : NOMBRES[i];
    }

    public int colorTexto() {
        return COLORES[hudColor];
    }

    public Estilo estilo(String id) {
        Estilo e = estilos.get(id);
        if (e == null) {
            e = new Estilo();
            e.fondo = hudFondo ? 40 : 0;
            e.sombra = hudSombra;
            e.colorTexto = hudColor;
            if (id.equals("teclas")) {
                e.escala = teclasEscala;
            }
            estilos.put(id, e);
        }
        return e;
    }

    /** Lista de mods extra de esa versión (null = todavía no se eligió: NexLauncher usa los recomendados). */
    public List<String> extrasDe(String version) {
        return extras.get(version);
    }

    public void ponerExtra(String version, String id, boolean activo, List<String> porDefecto) {
        List<String> l = extras.get(version);
        if (l == null) {
            l = new ArrayList<String>(porDefecto);
            extras.put(version, l);
        }
        l.remove(id);
        if (activo) {
            l.add(id);
        }
    }

    /** Hora del juego (0–23999) como texto: 6000 -> 12:00. */
    public static String textoHora(int ticks) {
        int minutos = ((ticks + 6000) % 24000) * 60 / 1000;
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }
}
