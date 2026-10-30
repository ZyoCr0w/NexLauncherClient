package nexlauncher.agente;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo llama authlib (ya cambiada por el agente). Solo usa clases de Java: vive en el cargador de arranque y
 * llega a las de Minecraft por reflexión, con el cargador del propio perfil.
 */
public final class Hooks {
    private static final String BASE = System.getProperty("nexlauncher.skins");
    private static final ConcurrentHashMap<String, Object[]> CACHE = new ConcurrentHashMap<String, Object[]>();
    private static final long VIGENTE = 60_000L, SIN_SKIN = 60_000L;  // cambios de skin: al volver a entrar
    private static final Pattern VALOR = Pattern.compile("\"value\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
    private static final Pattern FIRMA = Pattern.compile("\"signature\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
    private static final Pattern NOMBRE = Pattern.compile("\\w{1,16}");
    private static final java.util.Set<String> AVISADOS = java.util.Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>());

    private Hooks() {
    }

    /** Devuelve el mismo perfil, o uno igual con la skin de NexLauncher si el servidor no mandó ninguna. */
    public static Object preparar(Object perfil) {
        if (BASE == null || perfil == null) {
            return perfil;
        }
        try {
            ClassLoader cl = perfil.getClass().getClassLoader();
            Class<?> multimap = Class.forName("com.google.common.collect.Multimap", false, cl);
            Object props = llamar(perfil, "getProperties", "properties");
            if (props == null || (Boolean) multimap.getMethod("containsKey", Object.class).invoke(props, "textures")) {
                return perfil;
            }
            String nombre = (String) llamar(perfil, "getName", "name");
            if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
                return perfil;
            }
            String[] t = buscar(nombre);
            if (t == null) {
                return perfil;
            }
            if (AVISADOS.add(nombre.toLowerCase())) {
                System.out.println("[NexLauncher] Skin de NexLauncher para " + nombre);
            }
            Class<?> cProp = Class.forName("com.mojang.authlib.properties.Property", false, cl);
            Object prop = cProp.getConstructor(String.class, String.class, String.class).newInstance("textures", t[0], t[1]);
            Method put = multimap.getMethod("put", Object.class, Object.class);
            try {
                put.invoke(props, "textures", prop);
                return perfil;
            } catch (InvocationTargetException inmutable) {
                // authlib 7 (Minecraft 1.21.9+): el perfil no se puede cambiar, se arma una copia
            }
            Object mapa = Class.forName("com.google.common.collect.LinkedHashMultimap", false, cl).getMethod("create").invoke(null);
            multimap.getMethod("putAll", multimap).invoke(mapa, props);
            put.invoke(mapa, "textures", prop);
            Object nuevas = props.getClass().getConstructor(multimap).newInstance(mapa);
            UUID id = (UUID) llamar(perfil, "getId", "id");
            Constructor<?> k = perfil.getClass().getConstructor(UUID.class, String.class, props.getClass());
            return k.newInstance(id, nombre, nuevas);
        } catch (Throwable e) {
            return perfil;  // nunca romper el juego por una skin
        }
    }

    private static Object llamar(Object o, String... nombres) throws Exception {
        for (String n : nombres) {
            try {
                return o.getClass().getMethod(n).invoke(o);
            } catch (NoSuchMethodException e) {
                // siguiente nombre (cambió entre versiones de authlib)
            }
        }
        return null;
    }

    /** {valor, firma} de la propiedad "textures", o null. Pregunta a NexLauncher (que la busca en NexSkins). */
    static String[] buscar(String nombre) {
        String clave = nombre.toLowerCase();
        long ahora = System.currentTimeMillis();
        Object[] c = CACHE.get(clave);
        if (c != null && (Long) c[0] > ahora) {
            return (String[]) c[1];
        }
        String[] r = null;
        boolean ok = false;
        try {
            URL url = new URL(BASE + "/nexlauncher/perfil?name=" + URLEncoder.encode(nombre, "UTF-8"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            con.setConnectTimeout(1500);
            con.setReadTimeout(4000);
            int codigo = con.getResponseCode();
            if (codigo == 200) {
                InputStream in = con.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0 && out.size() < 65536) {
                    out.write(buf, 0, n);
                }
                in.close();
                String json = out.toString("UTF-8");
                Matcher v = VALOR.matcher(json), f = FIRMA.matcher(json);
                if (v.find()) {
                    r = new String[]{v.group(1), f.find() ? f.group(1) : null};
                }
                ok = true;
            } else {
                ok = codigo == 204 || codigo == 404;
            }
        } catch (Exception e) {
            // NexLauncher cerrado o sin red: se vuelve a intentar en un rato
        }
        CACHE.put(clave, new Object[]{ahora + (r != null ? VIGENTE : ok ? SIN_SKIN : 30_000L), r});
        return r;
    }
}
