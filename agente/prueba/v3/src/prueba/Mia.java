package prueba;
import com.mojang.authlib.GameProfile; import com.mojang.authlib.properties.*; import com.mojang.authlib.services.*; import java.util.*;
public class Mia {
  public static void main(String[] a) throws Exception {
    var s = MinecraftServicesSessionService.class.getDeclaredConstructor(); s.setAccessible(true); var ss = s.newInstance();
    String val = Base64.getEncoder().encodeToString("{\"profileName\":\"Otro\",\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/servidor\"}}}".getBytes());
    for (String n : new String[]{"Yo", "Amigo", "SinSkin"}) {
      var m = com.google.common.collect.LinkedHashMultimap.<String, Property>create(); m.put("textures", new Property("textures", val, "firma"));
      var p = ss.getPackedTextures(new GameProfile(UUID.randomUUID(), n, new PropertyMap(m)));
      String url = new String(Base64.getDecoder().decode(p.value())).replaceAll(".*\"url\"\\s*:\\s*\"([^\"]+)\".*", "$1");
      System.out.println(n + " -> " + url);
    }
  }
}
