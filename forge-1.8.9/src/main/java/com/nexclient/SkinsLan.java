package com.nexclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Skins de NexLauncher en tu mundo abierto en LAN (pestaña Amigos): cuando entra alguien sin skin (por
 * ejemplo una cuenta no premium), se la pide al launcher que abrió el juego, que guarda la tuya y las de los
 * amigos que se unieron con NexLauncher. Así todos los que usan NexLauncher ven las skins de todos.
 */
public final class SkinsLan {
    private SkinsLan() {
    }

    public static void completar(GameProfile perfil) {
        String base = System.getProperty("nexlauncher.skins");
        if (base == null || perfil == null || perfil.getName() == null || perfil.getProperties().containsKey("textures")) {
            return;
        }
        try {
            URL url = new URL(base + "/nexlauncher/perfil?name=" + URLEncoder.encode(perfil.getName(), "UTF-8"));
            HttpURLConnection c = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            c.setConnectTimeout(1000);
            c.setReadTimeout(2000);
            if (c.getResponseCode() != 200) {
                return;
            }
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0 && out.size() < 262144) {
                out.write(buf, 0, n);
            }
            in.close();
            JsonObject o = new JsonParser().parse(out.toString("UTF-8")).getAsJsonObject();
            for (JsonElement e : o.getAsJsonArray("properties")) {
                JsonObject p = e.getAsJsonObject();
                if ("textures".equals(p.get("name").getAsString())) {
                    String firma = p.has("signature") ? p.get("signature").getAsString() : null;
                    perfil.getProperties().put("textures", new Property("textures", p.get("value").getAsString(), firma));
                }
            }
        } catch (Exception e) {
            // sin skin: queda la de siempre
        }
    }
}
