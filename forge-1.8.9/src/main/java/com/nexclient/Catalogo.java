package com.nexclient;

import java.util.ArrayList;
import java.util.List;

/** Mods extra que NexLauncher puede instalar en este perfil PvP (ids de Modrinth). */
public final class Catalogo {
    public static final String VERSION = "1.8.9";
    /** Sin minimapas con radar ni barras de vida: los prohíben servidores como UniversoCraft. */
    public static final String[][] MODS = {
            // id, nombre, descripción, recomendado (1 = viene activado)
            {"zV5r3pPn", "3D Skin Layers", "Capas de skin en 3D", "0"},
            {"kYuIpRLv", "Wavey Capes", "Capas con movimiento", "0"},
            {"NNAgCjsB", "Entity Culling", "No dibuja entidades que no ves", "1"},
            {"Nv2fQJo5", "ReplayMod", "Graba tus partidas", "0"},
    };

    private Catalogo() {
    }

    public static List<String> porDefecto() {
        List<String> l = new ArrayList<String>();
        for (String[] m : MODS) {
            if (m[3].equals("1")) {
                l.add(m[0]);
            }
        }
        return l;
    }

    public static boolean elegido(Config c, String id) {
        List<String> l = c.extrasDe(VERSION);
        return (l == null ? porDefecto() : l).contains(id);
    }
}
