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
    private static final long VIGENTE = 30_000L, SIN_SKIN = 30_000L;  // cambios de skin: al volver a entrar
    private static final Pattern VALOR = Pattern.compile("\"value\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
    private static final Pattern FIRMA = Pattern.compile("\"signature\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");
    private static final Pattern NOMBRE = Pattern.compile("\\w{1,16}");
    private static final Pattern URL_TEX = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    /** Clave común de NexLauncher (la misma que firma las skins offline y las de NexSkins). */
    private static final String CLAVE_N = "e5d56c0d08ee4f1af8f3b604cb0b3eb23ce0cd177fc8ec331374b6e6b141d634a51ff9c3d65761e3a736a16f3b19b096a3060f8f9ee0bb9c48b70fd5dba498a45aacdefbdf339be3fb65532f9c30c451e8cd5e1bb4275c2ad45658a672f140dd48235197f12f7f7ae3a3ec0d4a3b1bd4f6ef5785a72ebd65df73039152c951b0372fd73fedcc02f0a0a7878d0a4071f3d4b1e55a58c8eb0534948eab68b8894e9856e49f72e50a9f93a839e015e2fb130585cfd15655f9f799f88f23a8ce11ceeb088ac96f1e5c11c7cc8616a9a9e8499260dae91bc919c0c4cf07b03ec55167291b5cf026ed031c1d1c8dff76146f1dbec295197e36a998a5e3e14ce373e083";
    private static volatile java.security.PublicKey llave;
    private static final ConcurrentHashMap<String, Boolean> FIRMAS = new ConcurrentHashMap<String, Boolean>();
    private static final String[] DOMINIOS = System.getProperty("nexlauncher.dominios", "").toLowerCase().split(",");
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
            if (props == null) {
                return perfil;
            }
            boolean reemplazar = (Boolean) multimap.getMethod("containsKey", Object.class).invoke(props, "textures");
            if (reemplazar && !deOtraPc(multimap.getMethod("get", Object.class).invoke(props, "textures"))) {
                return perfil;
            }
            String nombre = (String) llamar(perfil, "getName", "name");
            if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
                return perfil;
            }
            String[] t = buscar(nombre + (reemplazar ? "&mojang=1" : ""));
            if (t == null) {
                return perfil;
            }
            if (AVISADOS.add(nombre.toLowerCase())) {
                System.out.println("[NexLauncher] Skin de NexLauncher para " + nombre);
            }
            Class<?> cProp = Class.forName("com.mojang.authlib.properties.Property", false, cl);
            Object prop = cProp.getConstructor(String.class, String.class, String.class).newInstance("textures", t[0], t[1]);
            Method put = multimap.getMethod("put", Object.class, Object.class);
            Method quitar = reemplazar ? multimap.getMethod("removeAll", Object.class) : null;
            try {
                if (reemplazar) {
                    quitar.invoke(props, "textures");
                }
                put.invoke(props, "textures", prop);
                return perfil;
            } catch (InvocationTargetException inmutable) {
                // authlib 7 (Minecraft 1.21.9+): el perfil no se puede cambiar, se arma una copia
            }
            Object mapa = Class.forName("com.google.common.collect.LinkedHashMultimap", false, cl).getMethod("create").invoke(null);
            multimap.getMethod("putAll", multimap).invoke(mapa, props);
            if (reemplazar) {
                quitar.invoke(mapa, "textures");
            }
            put.invoke(mapa, "textures", prop);
            Object nuevas = props.getClass().getConstructor(multimap).newInstance(mapa);
            UUID id = (UUID) llamar(perfil, "getId", "id");
            Constructor<?> k = perfil.getClass().getConstructor(UUID.class, String.class, props.getClass());
            return k.newInstance(id, nombre, nuevas);
        } catch (Throwable e) {
            return perfil;  // nunca romper el juego por una skin
        }
    }

    private static final Pattern NOMBRE_TEX = Pattern.compile("\"profileName\"\\s*:\\s*\"(\\w{1,16})\"");

    /**
     * unpackTextures(Property): si la skin apunta al launcher de otra PC, devuelve la misma persona pero con la
     * skin que da tu launcher (NexSkins o su cuenta premium). No depende de cómo sea el perfil en cada versión.
     */
    public static Object propiedad(Object p) {
        if (BASE == null || p == null) {
            return p;
        }
        try {
            java.util.List<Object> una = java.util.Collections.singletonList(p);
            if (!deOtraPc(una)) {
                return p;
            }
            String v = (String) llamar(p, "getValue", "value");
            Matcher m = NOMBRE_TEX.matcher(new String(java.util.Base64.getDecoder().decode(v), "UTF-8"));
            if (!m.find()) {
                return p;
            }
            String nombre = m.group(1);
            String[] t = buscar(nombre + "&mojang=1");
            if (t == null) {
                return p;
            }
            if (AVISADOS.add(nombre.toLowerCase())) {
                System.out.println("[NexLauncher] Skin de NexLauncher para " + nombre + " (la del servidor apuntaba a otra PC)");
            }
            return p.getClass().getConstructor(String.class, String.class, String.class).newInstance("textures", t[0], t[1]);
        } catch (Throwable e) {
            return p;
        }
    }

    /** ¿La skin apunta al launcher de otra PC (http://127.0.0.1:otro-puerto)? Desde aquí no se puede bajar. */
    private static boolean deOtraPc(Object propiedades) {
        try {
            for (Object p : (Iterable<?>) propiedades) {
                String v = (String) llamar(p, "getValue", "value");
                if (v == null) {
                    continue;
                }
                Matcher m = URL_TEX.matcher(new String(java.util.Base64.getDecoder().decode(v), "UTF-8"));
                while (m.find()) {
                    String u = m.group(1);
                    if (local(new URL(u).getHost()) && !u.startsWith(BASE + "/")) {
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            // propiedad rara: se deja como venía
        }
        return false;
    }

    private static boolean local(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "[::1]".equals(host);
    }

    /** Lo llaman las revisiones de firma de authlib: true si la firmó NexLauncher (la de Mojang la revisa authlib). */
    public static boolean firma(Object propiedad) {
        if (BASE == null || propiedad == null) {
            return false;
        }
        try {
            String v = (String) llamar(propiedad, "getValue", "value");
            String f = (String) llamar(propiedad, "getSignature", "signature");
            if (v == null || f == null) {
                return false;
            }
            Boolean ok = FIRMAS.get(f);
            if (ok == null) {
                if (llave == null) {
                    llave = java.security.KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.RSAPublicKeySpec(
                            new java.math.BigInteger(CLAVE_N, 16), java.math.BigInteger.valueOf(65537)));
                }
                java.security.Signature s = java.security.Signature.getInstance("SHA1withRSA");
                s.initVerify(llave);
                s.update(v.getBytes("UTF-8"));
                ok = s.verify(java.util.Base64.getDecoder().decode(f));
                if (FIRMAS.size() > 4096) {
                    FIRMAS.clear();
                }
                FIRMAS.put(f, ok);
            }
            return ok;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Lo llaman las listas de dominios de texturas: true para el launcher, NexSkins y los túneles de amigos. */
    public static boolean dominio(String url) {
        if (BASE == null || url == null) {
            return false;
        }
        try {
            String h = new URL(url).getHost().toLowerCase();
            if (local(h)) {
                return true;
            }
            for (String d : DOMINIOS) {
                if (!d.isEmpty() && (h.equals(d) || (d.startsWith(".") && (h.endsWith(d) || h.equals(d.substring(1)))))) {
                    return true;
                }
            }
        } catch (Throwable e) {
            // URL inválida: que authlib decida
        }
        return false;
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
            String[] partes = nombre.split("&", 2);
            URL url = new URL(BASE + "/nexlauncher/perfil?name=" + URLEncoder.encode(partes[0], "UTF-8")
                    + (partes.length > 1 ? "&" + partes[1] : ""));
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
