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
 * Solo toca dos métodos de YggdrasilMinecraftSessionService: al principio les cambia el perfil por uno con la
 * skin de NexLauncher si el servidor no mandó ninguna. Si algo falla, el juego sigue exactamente igual.
 */
public final class Agente {
    static final String CLASE = "com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService";

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
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader cl, String nombre, Class<?> redefinida, ProtectionDomain pd, byte[] bytes) {
                if (!CLASE.equals(nombre)) {
                    return null;
                }
                try {
                    // Si el cargador de authlib no ve Hooks, no se toca nada (mejor sin skins que sin juego)
                    Class.forName("nexlauncher.agente.Hooks", false, cl);
                    byte[] nuevo = Parche.aplicar(bytes);
                    System.out.println("[NexLauncher] Skins de NexLauncher activadas en authlib"
                            + (nuevo == null ? " (sin métodos que cambiar)" : "."));
                    return nuevo;
                } catch (Throwable e) {
                    System.err.println("[NexLauncher] No se pudo preparar authlib para las skins: " + e);
                    return null;
                }
            }
        });
    }
}
