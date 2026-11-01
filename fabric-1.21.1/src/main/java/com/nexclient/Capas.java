package com.nexclient;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capas animadas en el juego: la GIF de cada jugador y las de Nex. Se preparan en otro hilo y se suben a la tarjeta
 * de video de a pocos cuadros (sin tirones); cada cuadro se dibuja según la hora, así todas van sincronizadas.
 */
public final class Capas {
    private static Object mcJugador() {
        return Minecraft.getInstance().player;
    }

    private static final class Cargada {
        volatile CapasAnimadas.Animacion anim;     // null mientras se prepara
        volatile boolean fallo;
        final List<ResourceLocation> texturas = new ArrayList<ResourceLocation>();
    }

    private static final Map<String, Cargada> CAPAS = new ConcurrentHashMap<String, Cargada>();
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NexClient-capas");
        t.setDaemon(true);
        return t;
    });

    private Capas() {
    }

    /** Textura de capa que toca ahora para este jugador, o null para dejar la suya. Se llama al dibujar. */
    public static ResourceLocation capa(AbstractClientPlayer p) {
        Minecraft mc = Minecraft.getInstance();
        Cosmeticos.Jugador j = Cosmeticos.de(p.getGameProfile().getName());
        if (j == null || !Cosmeticos.mostrar(p == mc.player)) {
            return null;
        }
        String id = j.de("capa");
        if (id == null || Cosmeticos3D.hueso(id) != null) {  // las capas 3D las dibuja CapaCosmeticos
            return null;
        }
        String clave = "gif".equals(id) ? (j.gif == null ? null : "gif_" + j.gif) : id;
        if (clave == null) {
            return null;
        }
        Cargada c = CAPAS.get(clave);
        if (c == null) {
            c = preparar(clave, id, j.gif);
        }
        CapasAnimadas.Animacion a = c.anim;
        if (a == null) {
            return null;
        }
        // de a 3 cuadros por llamada: subirlos todos de golpe se notaría como un tirón
        for (int n = 0; n < 3 && c.texturas.size() < a.cuadros.size(); n++) {
            c.texturas.add(subir(clave, c.texturas.size(), a.ancho, a.alto, a.cuadros.get(c.texturas.size())));
        }
        if (c.texturas.size() < a.cuadros.size()) {
            return c.texturas.isEmpty() ? null : c.texturas.get(0);
        }
        return c.texturas.get(a.indice(System.currentTimeMillis()));
    }

    /** ¿Lleva una capa 3D de NexLauncher? Entonces no se dibuja su capa normal (quedaría encima de la otra). */
    public static boolean capaModelo(AbstractClientPlayer p) {
        Cosmeticos.Jugador j = Cosmeticos.de(p.getGameProfile().getName());
        if (j == null || !Cosmeticos.mostrar(p == mcJugador())) {
            return false;
        }
        String id = j.de("capa");
        return id != null && Cosmeticos3D.hueso(id) != null;
    }

    private static Cargada preparar(final String clave, final String id, final String hash) {
        final Cargada c = new Cargada();
        CAPAS.put(clave, c);
        HILO.execute(() -> {
            try {
                if ("capa_brillo".equals(id)) {
                    c.anim = CapasAnimadas.brillo();
                } else if ("capa_color".equals(id)) {
                    c.anim = CapasAnimadas.color();
                } else if (hash != null && Cosmeticos.base() != null) {
                    byte[] b = Cosmeticos.leer(Cosmeticos.base() + "/nexlauncher/cos/gif/" + hash, 2 << 20);
                    Gif g = b == null ? null : Gif.leer(b, 64);
                    if (g != null) {
                        c.anim = CapasAnimadas.deGif(g);
                    } else {
                        c.fallo = true;
                    }
                }
            } catch (Exception e) {
                c.fallo = true;
            }
            if (c.fallo) {  // se vuelve a intentar dentro de un rato
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ignorada) {
                    Thread.currentThread().interrupt();
                }
                CAPAS.remove(clave, c);
            }
        });
        return c;
    }

    private static ResourceLocation subir(String clave, int i, int w, int h, int[] argb) {
        NativeImage img = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = argb[y * w + x];
                // NativeImage guarda ABGR
                img.setPixelRGBA(x, y, (v & 0xFF00FF00) | ((v >> 16) & 0xFF) | ((v & 0xFF) << 16));
            }
        }
        ResourceLocation r = ResourceLocation.fromNamespaceAndPath("nexclient",
                "capas/" + clave.toLowerCase().replaceAll("[^a-z0-9_]", "_") + "/" + i);
        Minecraft.getInstance().getTextureManager().register(r, new DynamicTexture(img));
        return r;
    }
}
