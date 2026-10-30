package com.mojang.authlib.services;
import java.net.*; import java.util.*;
public class MinecraftServicesDiscoveryService {
  final List<String> permitidos = List.of(".minecraft.net", ".mojang.com");
  public boolean isAllowedTextureDomain(String url) {
    String h;
    try { h = new URI(url).getHost(); } catch (URISyntaxException e) { return false; }
    for (String d : permitidos) if (h.endsWith(d)) return true;
    return false;
  }
}
