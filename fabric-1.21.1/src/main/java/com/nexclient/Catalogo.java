package com.nexclient;

import java.util.ArrayList;
import java.util.List;

/** Mods extra que NexLauncher puede instalar en este perfil PvP (ids de Modrinth). */
public final class Catalogo {
    public static final String VERSION = "1.21.1";
    public static final String[][] MODS = {
            // id, nombre, descripción, recomendado (1 = viene activado)
            {"YL57xq9U", "Iris Shaders", "Shaders con Sodium", "0"},
            {"zV5r3pPn", "3D Skin Layers", "Capas de skin en 3D", "0"},
            {"MPCX6s5C", "Not Enough Anim.", "Not Enough Animations: animaciones en 3.ª persona", "0"},
            {"1IjD5062", "Continuity", "Texturas conectadas", "0"},
            {"BVzZfTc1", "ETF", "Entity Texture Features: texturas de packs", "0"},
            {"1bokaNcj", "Minimapa", "Xaero's Minimap", "0"},
            {"NcUtCpym", "Mapa del mundo", "Xaero's World Map (tecla M)", "0"},
            {"EsAfCjCV", "AppleSkin", "Saturación en la barra", "0"},
            {"gvQqBUqZ", "Lithium", "Rendimiento", "1"},
            {"NNAgCjsB", "Entity Culling", "Rendimiento", "1"},
            {"uXXizFIs", "FerriteCore", "Menos RAM", "1"},
            {"5ZwdcRci", "ImmediatelyFast", "Rendimiento", "1"},
            {"51shyZVL", "More Culling", "Rendimiento", "0"},
            {"PtjYWJkn", "Sodium Extra", "Más opciones de video", "0"},
            {"LQ3K71Q1", "Dynamic FPS", "Menos FPS en segundo plano", "0"},
            {"mOgUt4GM", "Mod Menu", "Lista de mods", "0"},
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

    public static List<String> ids() {
        List<String> l = new ArrayList<String>();
        for (String[] m : MODS) {
            l.add(m[0]);
        }
        return l;
    }
}
