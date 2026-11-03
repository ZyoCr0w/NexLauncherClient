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
    /** Tu nombre si elegiste «ver siempre mi skin del launcher»: tu jugador se ve con ella (solo en tu juego). */
    private static final String MI_NOMBRE = System.getProperty("nexlauncher.miskin");
    /** «Skins del launcher encima»: a quien tenga skin de NexLauncher se le pone esa aunque el servidor mande otra. */
    private static final boolean ENCIMA = Boolean.getBoolean("nexlauncher.encima") || MI_NOMBRE != null;
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
            String nombre = (String) llamar(perfil, "getName", "name");
            if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
                return perfil;
            }
            Object texturas = multimap.getMethod("get", Object.class).invoke(props, "textures");
            boolean otraPc = reemplazar && deOtraPc(texturas);
            boolean mia = MI_NOMBRE != null && nombre.equalsIgnoreCase(MI_NOMBRE);
            // «skins del launcher encima»: el servidor mandó una (p. ej. SkinsRestorer) pero se usa la de NexLauncher
            boolean encima = reemplazar && !otraPc && ENCIMA;
            if (reemplazar && !otraPc && !encima) {
                return perfil;
            }
            String extra = "";
            if (encima) {
                extra = "&encima=1";
            } else if (otraPc) {
                for (Object p : (Iterable<?>) texturas) {
                    extra = datosOtraPc((String) llamar(p, "getValue", "value"));
                    break;
                }
            }
            String[] t = buscar(nombre + extra);
            if (encima && (t == null || !conTextura(t[0]))) {
                if (mia && AVISADOS.add("?" + nombre.toLowerCase())) {
                    System.out.println("[NexLauncher] Skins encima: " + nombre + " no tiene skin de NexLauncher ("
                            + (t == null ? "sin respuesta" : "sin textura") + "); se queda la del servidor.");
                }
                return perfil;  // sin skin de NexLauncher: la del servidor
            }
            if (encima && AVISADOS.add("=" + nombre.toLowerCase())) {
                System.out.println("[NexLauncher] Skins encima: " + nombre + " usa su skin de NexLauncher en lugar de la del servidor.");
            }
            if (t == null && reemplazar) {
                avisarSinSkin(nombre);
            }
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
            String[] t = buscar(nombre + datosOtraPc(v));
            if (t == null) {
                avisarSinSkin(nombre);
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

    private static final Pattern SKIN_URL = Pattern.compile("\"SKIN\"\\s*:\\s*\\{\\s*\"url\"\\s*:\\s*\"[^\"]*/([0-9a-f]{64})\"");
    private static final Pattern CAPA_URL = Pattern.compile("\"CAPE\"\\s*:\\s*\\{\\s*\"url\"\\s*:\\s*\"[^\"]*/([0-9a-f]{64})\"");
    private static final Pattern SLIM = Pattern.compile("\"model\"\\s*:\\s*\"slim\"");

    /**
     * Lo que se le pasa a tu launcher de una skin que apunta a otra PC: sus hashes. Tu launcher la baja por el
     * mismo túnel del mundo de tu amigo (o de NexSkins / su cuenta premium) y te da un perfil que sí se puede usar.
     */
    private static String datosOtraPc(String valor) {
        StringBuilder q = new StringBuilder("&mojang=1");
        try {
            String json = new String(java.util.Base64.getDecoder().decode(valor), "UTF-8");
            Matcher m = SKIN_URL.matcher(json);
            if (m.find()) {
                q.append("&tex=").append(m.group(1));
            }
            m = CAPA_URL.matcher(json);
            if (m.find()) {
                q.append("&capa=").append(m.group(1));
            }
            if (SLIM.matcher(json).find()) {
                q.append("&modelo=slim");
            }
        } catch (Throwable e) {
            // sin hashes: se busca solo por nombre
        }
        return q.toString();
    }

    private static boolean conTextura(String valor) {
        try {
            return URL_TEX.matcher(new String(java.util.Base64.getDecoder().decode(valor), "UTF-8")).find();
        } catch (Throwable e) {
            return false;
        }
    }

    private static void avisarSinSkin(String nombre) {
        if (AVISADOS.add("!" + nombre.toLowerCase())) {
            System.out.println("[NexLauncher] La skin de " + nombre + " apunta al launcher de otra PC y no se pudo "
                    + "conseguir (ni por el servidor, ni en NexSkins, ni premium).");
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

    // ---- Tu mundo abierto en LAN: quién entra (hasJoinedServer)
    private static final ThreadLocal<Object> UNIDO = new ThreadLocal<Object>();
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{32,36})\"");
    private static final Pattern NOMBRE_J = Pattern.compile("\"name\"\\s*:\\s*\"(\\w{1,16})\"");

    /**
     * hasJoinedServer, antes de preguntar a Mojang: NexLauncher responde por tu mundo. Él revisa primero con Mojang
     * (premium de verdad: su perfil real) y, si en Amigos está «Dejar entrar cuentas no premium», da el perfil
     * offline. true = hay respuesta (la devuelve unidoResultado); false = sigue el código de authlib.
     */
    public static boolean unido(Object servicio, Object usuario, Object serverId) {
        UNIDO.remove();
        if (BASE == null || servicio == null || usuario == null || !(serverId instanceof String)) {
            return false;
        }
        try {
            String nombre = usuario instanceof String ? (String) usuario : (String) llamar(usuario, "getName", "name");
            if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
                return false;
            }
            URL url = new URL(BASE + "/sessionserver/session/minecraft/hasJoined?username="
                    + URLEncoder.encode(nombre, "UTF-8") + "&serverId=" + URLEncoder.encode((String) serverId, "UTF-8"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            con.setConnectTimeout(1500);
            con.setReadTimeout(8000);  // NexLauncher pregunta antes a Mojang
            if (con.getResponseCode() != 200) {
                return false;
            }
            InputStream in = con.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0 && out.size() < 65536) {
                out.write(buf, 0, n);
            }
            in.close();
            String json = out.toString("UTF-8");
            Matcher mi = ID.matcher(json), mn = NOMBRE_J.matcher(json);
            if (!mi.find()) {
                return false;
            }
            String hex = mi.group(1).replace("-", "");
            UUID id = new UUID(Long.parseUnsignedLong(hex.substring(0, 16), 16), Long.parseUnsignedLong(hex.substring(16), 16));
            String nom = mn.find() ? mn.group(1) : nombre;
            ClassLoader cl = servicio.getClass().getClassLoader();
            Class<?> cGp = Class.forName("com.mojang.authlib.GameProfile", false, cl);
            Object perfil;
            try {
                perfil = cGp.getConstructor(UUID.class, String.class).newInstance(id, nom);
            } catch (NoSuchMethodException e) {  // authlib 7: record (UUID, String, PropertyMap)
                Class<?> multimap = Class.forName("com.google.common.collect.Multimap", false, cl);
                Class<?> cMapa = Class.forName("com.mojang.authlib.properties.PropertyMap", false, cl);
                Object vacio = Class.forName("com.google.common.collect.LinkedHashMultimap", false, cl).getMethod("create").invoke(null);
                perfil = cGp.getConstructor(UUID.class, String.class, cMapa)
                        .newInstance(id, nom, cMapa.getConstructor(multimap).newInstance(vacio));
            }
            Matcher v = VALOR.matcher(json), f = FIRMA.matcher(json);
            if (v.find() && json.contains("\"textures\"")) {
                perfil = conTexturas(perfil, cl, v.group(1), f.find() ? f.group(1) : null);
            }
            Object r = perfil;
            Class<?> tipo = null;
            for (Method m : servicio.getClass().getMethods()) {
                if ("hasJoinedServer".equals(m.getName())) {
                    tipo = m.getReturnType();
                    break;
                }
            }
            if (tipo != null && !tipo.isInstance(perfil)) {  // authlib 4+: ProfileResult(GameProfile[, Set])
                try {
                    r = tipo.getConstructor(cGp).newInstance(perfil);
                } catch (NoSuchMethodException e) {
                    r = tipo.getConstructor(cGp, java.util.Set.class).newInstance(perfil, java.util.Collections.emptySet());
                }
            }
            if (AVISADOS.add("lan:" + nom.toLowerCase())) {
                System.out.println("[NexLauncher] Mundo LAN: entra " + nom + " (" + id + ") con el visto bueno de NexLauncher.");
            }
            UNIDO.set(r);
            return true;
        } catch (Throwable e) {
            return false;  // que decida authlib (Mojang)
        }
    }

    public static Object unidoResultado() {
        Object r = UNIDO.get();
        UNIDO.remove();
        return r;
    }

    private static Object conTexturas(Object perfil, ClassLoader cl, String valor, String firma) throws Exception {
        Class<?> multimap = Class.forName("com.google.common.collect.Multimap", false, cl);
        Object props = llamar(perfil, "getProperties", "properties");
        Object prop = Class.forName("com.mojang.authlib.properties.Property", false, cl)
                .getConstructor(String.class, String.class, String.class).newInstance("textures", valor, firma);
        Method put = multimap.getMethod("put", Object.class, Object.class);
        try {
            put.invoke(props, "textures", prop);
            return perfil;
        } catch (InvocationTargetException inmutable) {
            // authlib 7: perfil inmutable, se arma uno nuevo con sus propiedades
        }
        Object mapa = Class.forName("com.google.common.collect.LinkedHashMultimap", false, cl).getMethod("create").invoke(null);
        put.invoke(mapa, "textures", prop);
        Object nuevas = props.getClass().getConstructor(multimap).newInstance(mapa);
        return perfil.getClass().getConstructor(UUID.class, String.class, props.getClass())
                .newInstance(llamar(perfil, "getId", "id"), llamar(perfil, "getName", "name"), nuevas);
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

    private static final java.util.concurrent.ExecutorService FONDO = java.util.concurrent.Executors.newFixedThreadPool(2,
            new java.util.concurrent.ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "NexLauncher-skins");
                    t.setDaemon(true);
                    return t;
                }
            });
    private static final java.util.Set<String> PENDIENTES = java.util.Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>());

    /** El hilo que dibuja el juego: ahí nunca se espera a la red (se notaría como tirones). */
    private static boolean hiloDelJuego() {
        String n = Thread.currentThread().getName();
        return "Client thread".equals(n) || "Render thread".equals(n) || "main".equals(n);
    }

    /**
     * {valor, firma} de la propiedad "textures", o null. Pregunta a NexLauncher (que la busca en NexSkins o por el
     * túnel del amigo). Desde el hilo del juego no espera: la pide en segundo plano y la usa la próxima vez.
     */
    static String[] buscar(final String nombre) {
        String clave = nombre.toLowerCase();
        long ahora = System.currentTimeMillis();
        Object[] c = CACHE.get(clave);
        if (c != null && (Long) c[0] > ahora) {
            return (String[]) c[1];
        }
        if (hiloDelJuego()) {
            if (PENDIENTES.add(clave)) {
                FONDO.execute(new Runnable() {
                    public void run() {
                        try {
                            pedir(nombre);
                        } finally {
                            PENDIENTES.remove(nombre.toLowerCase());
                        }
                    }
                });
            }
            return c != null ? (String[]) c[1] : null;
        }
        return pedir(nombre);
    }

    private static String[] pedir(String nombre) {
        String clave = nombre.toLowerCase();
        long ahora = System.currentTimeMillis();
        String[] r = null;
        boolean ok = false;
        try {
            String[] partes = nombre.split("&", 2);
            URL url = new URL(BASE + "/nexlauncher/perfil?name=" + URLEncoder.encode(partes[0], "UTF-8")
                    + (partes.length > 1 ? "&" + partes[1] : ""));
            HttpURLConnection con = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            con.setConnectTimeout(1500);
            con.setReadTimeout(10000);  // puede bajar la skin por el túnel del amigo
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
