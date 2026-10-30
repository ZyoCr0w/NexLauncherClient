package nexlauncher.agente;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

/**
 * Agente de NexLauncher: cuando un servidor manda a un jugador sin skin (servidores no premium), el juego le
 * pregunta a NexLauncher, que la busca en NexSkins. Funciona en cualquier versión (también Vanilla), como el
 * sistema de skins de TLauncher, porque cambia authlib (la librería de cuentas de Minecraft) al cargarse.
 *
 * Toca pocos métodos de authlib: getTextures/getPackedTextures (perfil con la skin de NexLauncher si el servidor
 * no mandó ninguna o la mandó apuntando al launcher de otra PC) y las revisiones de firma y dominio de las
 * texturas (que acepten las de NexLauncher también con cuenta premium). Si algo falla, el juego sigue igual.
 */
public final class Agente {
    static final String VERSION = "3";
    static final String PAQUETE = "com/mojang/authlib/";

    private Agente() {
    }

    /** args: ruta del jar con Hooks (va aparte: el agente no puede compartir cargador con él). */
    public static void premain(String args, Instrumentation inst) {
        try {
            iniciar(args, inst);
        } catch (Throwable e) {  // un error aquí cerraría la JVM: el juego tiene que abrir igual
            System.err.println("[NexLauncher] Agente de skins desactivado: " + e);
        }
    }

    private static void iniciar(String args, Instrumentation inst) throws Exception {
        if (args == null || !new File(args).isFile()) {
            System.err.println("[NexLauncher] Agente de skins sin el jar de Hooks: desactivado.");
            return;
        }
        // Hooks va al cargador de arranque: así lo ve authlib aunque la cargue Forge, Fabric o un módulo
        inst.appendToBootstrapClassLoaderSearch(new JarFile(args));
        System.out.println("[NexLauncher] Agente de skins v" + VERSION + " cargado.");
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader cl, String nombre, Class<?> redefinida, ProtectionDomain pd, byte[] bytes) {
                // Barato: solo clases de authlib que mencionen los métodos de texturas, firmas o dominios
                if (nombre == null || !nombre.startsWith(PAQUETE) || !(contiene(bytes, "Textures")
                        || contiene(bytes, "validateProperty") || contiene(bytes, "isSignatureValid")
                        || contiene(bytes, "Domain"))) {
                    return null;
                }
                try {
                    synchronized (Parche.class) {
                        byte[] nuevo = Parche.aplicar(bytes);
                        if (nuevo == null) {
                            return null;
                        }
                        // Si el cargador de authlib no ve Hooks, no se toca nada (mejor sin skins que sin juego)
                        Class.forName("nexlauncher.agente.Hooks", false, cl);
                        System.out.println("[NexLauncher] Skins de NexLauncher activadas en " + nombre.replace('/', '.')
                                + " " + Parche.CAMBIADOS);
                        return nuevo;
                    }
                } catch (Throwable e) {
                    System.err.println("[NexLauncher] No se pudo preparar " + nombre + " para las skins: " + e);
                    return null;
                }
            }
        });
    }

    private static boolean contiene(byte[] b, String texto) {
        byte[] t = texto.getBytes();
        busqueda:
        for (int i = 0; i + t.length <= b.length; i++) {
            for (int j = 0; j < t.length; j++) {
                if (b[i + j] != t[j]) {
                    continue busqueda;
                }
            }
            return true;
        }
        return false;
    }
}
