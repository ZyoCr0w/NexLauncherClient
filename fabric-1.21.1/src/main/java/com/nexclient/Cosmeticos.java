package com.nexclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Etiquetas y cosméticos de los jugadores que tienes cerca. Se los pide a NexLauncher (que los pide a NexSkins con
 * tu sesión de NexLauncher, así el token no entra al juego). Solo responde en los perfiles PvP.
 * Mismo archivo en 1.8.9 y 1.21.1 (Java 8, gson antiguo).
 */
public final class Cosmeticos {
    /** Lo que lleva un jugador. {@code cache} lo usa cada versión para guardar lo ya convertido (texto, texturas). */
    public static final class Jugador {
        public final String nombre;
        public final String etiqueta;          // JSON de texto de Minecraft, o null
        public final Map<String, String> equipado;  // categoría -> id (la capa GIF: "gif")
        public final String gif;               // hash de su capa GIF, o null
        /** Logo de NexLauncher encima del nombre: "creador" (arcoíris), "premium" (verde), "offline" (gris) o null. */
        public final String insignia;
        public final Map<String, Object> cache = new ConcurrentHashMap<String, Object>();

        Jugador(String nombre, String etiqueta, Map<String, String> equipado, String gif, String insignia) {
            this.nombre = nombre;
            this.etiqueta = etiqueta;
            this.equipado = equipado;
            this.gif = gif;
            this.insignia = insignia;
        }

        public String de(String categoria) {
            return equipado.get(categoria);
        }
    }

    private static final Map<String, Jugador> DATOS = new ConcurrentHashMap<String, Jugador>();
    private static final Set<String> CONSULTADOS = new HashSet<String>();
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NexClient-cosmeticos");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean ocupado;
    private static long siguiente, ultimaNueva;
    private static String servidorAntes = "";

    private Cosmeticos() {
    }

    public static String base() {
        return System.getProperty("nexlauncher.skins");
    }

    public static Jugador de(String nombre) {
        return nombre == null ? null : DATOS.get(nombre.toLowerCase());
    }

    /**
     * Se llama en cada tick con tu nombre, el servidor y los jugadores que tienes cerca. Pregunta cada 10 s (y
     * enseguida si aparece alguien nuevo, como mucho cada 2 s). La consulta va en otro hilo.
     */
    public static void tick(String yo, String servidor, Collection<String> cerca) {
        if (base() == null || yo == null || ocupado) {
            return;
        }
        servidor = servidor == null ? "" : servidor;
        long ahora = System.currentTimeMillis();
        if (!servidor.equals(servidorAntes)) {  // otro servidor: lo de antes ya no vale
            servidorAntes = servidor;
            DATOS.clear();
            CONSULTADOS.clear();
            siguiente = 0;
        }
        boolean nueva = false;
        for (String n : cerca) {
            if (!CONSULTADOS.contains(n.toLowerCase())) {
                nueva = true;
                break;
            }
        }
        if (ahora < siguiente && !(nueva && ahora - ultimaNueva > 2000)) {
            return;
        }
        if (nueva) {
            ultimaNueva = ahora;
        }
        siguiente = ahora + 10000;
        final Set<String> nombres = new HashSet<String>();
        nombres.add(yo);
        for (String n : cerca) {
            if (nombres.size() >= 100) {
                break;
            }
            nombres.add(n);
        }
        for (String n : nombres) {
            CONSULTADOS.add(n.toLowerCase());
        }
        final String yoF = yo, srv = servidor;
        ocupado = true;
        HILO.execute(() -> {
            try {
                consultar(yoF, srv, nombres);
            } catch (Exception e) {
                // launcher cerrado o sin red: se intenta en la próxima vuelta
            } finally {
                ocupado = false;
            }
        });
    }

    private static void consultar(String yo, String servidor, Set<String> nombres) throws Exception {
        StringBuilder n = new StringBuilder();
        for (String s : nombres) {
            if (n.length() > 0) {
                n.append(',');
            }
            n.append(s);
        }
        byte[] r = leer(base() + "/nexlauncher/cos/lote?yo=" + URLEncoder.encode(yo, "UTF-8") + "&srv="
                + URLEncoder.encode(servidor, "UTF-8") + "&n=" + URLEncoder.encode(n.toString(), "UTF-8"), 65536 * 4);
        if (r == null) {
            return;
        }
        JsonObject o = new JsonParser().parse(new String(r, "UTF-8")).getAsJsonObject();
        JsonObject jug = o.has("jugadores") && o.get("jugadores").isJsonObject() ? o.getAsJsonObject("jugadores")
                : new JsonObject();
        for (String nombre : nombres) {
            String clave = nombre.toLowerCase();
            JsonElement e = jug.get(clave);
            if (e == null || !e.isJsonObject()) {
                DATOS.remove(clave);
                continue;
            }
            JsonObject d = e.getAsJsonObject();
            String etiqueta = d.has("etiqueta") && !d.get("etiqueta").isJsonNull() ? d.get("etiqueta").toString() : null;
            String insignia = d.has("insignia") && d.get("insignia").isJsonPrimitive() ? d.get("insignia").getAsString() : null;
            Map<String, String> eq = new HashMap<String, String>();
            String gif = null;
            if (d.has("equipado") && d.get("equipado").isJsonObject()) {
                for (Map.Entry<String, JsonElement> x : d.getAsJsonObject("equipado").entrySet()) {
                    JsonElement v = x.getValue();
                    if (v.isJsonPrimitive()) {
                        eq.put(x.getKey(), v.getAsString());
                    } else if (v.isJsonObject() && "capa".equals(x.getKey()) && v.getAsJsonObject().has("hash")) {
                        eq.put("capa", "gif");
                        gif = v.getAsJsonObject().get("hash").getAsString();
                    }
                }
            }
            Jugador antes = DATOS.get(clave);
            if (antes != null && eq.equals(antes.equipado) && (etiqueta == null ? antes.etiqueta == null
                    : etiqueta.equals(antes.etiqueta)) && (gif == null ? antes.gif == null : gif.equals(antes.gif))
                    && (insignia == null ? antes.insignia == null : insignia.equals(antes.insignia))) {
                continue;  // igual que antes: se conserva lo ya convertido (texto, texturas)
            }
            DATOS.put(clave, new Jugador(nombre, etiqueta, eq, gif, insignia));
            System.out.println("[NexClient] Cosméticos de " + nombre + ": etiqueta " + etiqueta + ", " + eq);
        }
    }

    /** GET al launcher; null si no responde 200. */
    public static byte[] leer(String direccion, int maximo) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(direccion).openConnection(Proxy.NO_PROXY);
        c.setConnectTimeout(1500);
        c.setReadTimeout(10000);
        if (c.getResponseCode() != 200) {
            return null;
        }
        InputStream in = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int k;
        while ((k = in.read(buf)) > 0) {
            out.write(buf, 0, k);
            if (out.size() > maximo) {
                in.close();
                return null;
            }
        }
        in.close();
        return out.toByteArray();
    }

    /** ¿Se dibujan los cosméticos de este jugador? (Config: 0 todos, 1 solo los míos, 2 ninguno). */
    public static boolean mostrar(boolean esYo) {
        int m = Config.get().cosmeticosVer;
        return m == 0 || (m == 1 && esYo);
    }

    /** Bloques que sube el nombre de este jugador para no encimarse con su cosmético de cabeza (0 si no lleva). */
    public static float subidaNombre(String nombre, boolean esYo) {
        Jugador j = de(nombre);
        if (j == null || !mostrar(esYo)) {
            return 0;
        }
        return Cosmeticos3D.subidaNombre(j.de("cabeza"));
    }
}
