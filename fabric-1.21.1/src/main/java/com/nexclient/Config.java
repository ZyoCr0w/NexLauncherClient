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

/**
 * Ajustes de NexClient. El mismo archivo (.minecraft/config/nexclient.json) lo usan la versión 1.8.9,
 * la 1.21.1 y la pestaña Ajustes de NexLauncher, así que se configura una sola vez.
 */
public class Config {
    // ---- HUD
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

    // ---- Blockhit
    public boolean blockhit = true;
    /** true: la espada se sigue moviendo al golpear mientras bloqueas (como en 1.7). */
    public boolean estilo17 = true;
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

    public static final String[] SONIDOS = {"metal", "escudo", "golpe", "orbe", "pling", "click"};
    public static final String[] NOMBRES = {"Metal", "Escudo", "Golpe", "Experiencia", "Pling", "Click"};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config actual = new Config();
    private static File archivo;

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
        }
        actual = c != null ? c : new Config();
        actual.limitar();
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
        } catch (Exception e) {
            System.err.println("[NexClient] No se pudo guardar nexclient.json: " + e);
        }
    }

    private void limitar() {
        teclasEscala = Math.max(0.5f, Math.min(2.5f, teclasEscala));
        multiplicador = Math.max(1, Math.min(10, multiplicador));
        volumen = Math.max(0f, Math.min(2f, volumen));
        tono = Math.max(0.5f, Math.min(2f, tono));
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
}
