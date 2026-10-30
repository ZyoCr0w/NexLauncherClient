package com.nexclient;

import java.util.ArrayList;
import java.util.List;

/** Mods extra que NexLauncher puede instalar en este perfil PvP (ids de Modrinth). */
public final class Catalogo {
    public static final String VERSION = "1.21.1";
    /** Sin minimapas con radar ni barras de vida: los prohíben servidores como UniversoCraft. */
    public static final String[][] MODS = {
            // id, nombre, descripción, recomendado (1 = viene activado)
            {"YL57xq9U", "Iris Shaders", "Shaders con Sodium", "0"},
            {"89Wsn8GD", "Capas", "Capes: ves las capas de OptiFine, MinecraftCapes y más, y los demás ven la tuya", "1"},
            {"zV5r3pPn", "3D Skin Layers", "Capas de skin en 3D", "1"},
            {"kYuIpRLv", "Wavey Capes", "Capas con movimiento", "1"},
            {"idMHQ4n2", "Skins NexLauncher", "CustomSkinLoader: ves las skins offline de NexLauncher en cualquier servidor", "1"},
            {"MPCX6s5C", "Not Enough Anim.", "Not Enough Animations: animaciones en 3.ª persona", "0"},
            {"1IjD5062", "Continuity", "Texturas conectadas", "0"},
            {"BVzZfTc1", "ETF", "Entity Texture Features: texturas de packs", "0"},
            {"EsAfCjCV", "AppleSkin", "Saturación en la barra", "0"},
            {"gvQqBUqZ", "Lithium", "Rendimiento", "1"},
            {"NNAgCjsB", "Entity Culling", "Rendimiento", "1"},
            {"uXXizFIs", "FerriteCore", "Menos RAM", "1"},
            {"5ZwdcRci", "ImmediatelyFast", "Rendimiento", "1"},
            {"nmDcB62a", "ModernFix", "Abre más rápido y usa menos RAM", "1"},
            {"g96Z4WVZ", "BadOptimizations", "Optimizaciones de luz, cielo y texturas", "1"},
            {"51shyZVL", "More Culling", "Rendimiento", "1"},
            {"PtjYWJkn", "Sodium Extra", "Más opciones de video", "1"},
            {"LQ3K71Q1", "Dynamic FPS", "Menos FPS en segundo plano", "1"},
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
