package com.mojang.authlib.services;
import com.mojang.authlib.GameProfile; import com.mojang.authlib.properties.*; import java.util.*; import java.util.regex.*;
public class MinecraftServicesSessionService {
  final List<MinecraftServicesKeyInfo> keys; final MinecraftServicesDiscoveryService disc = new MinecraftServicesDiscoveryService();
  MinecraftServicesSessionService() throws Exception { keys = List.of(new MinecraftServicesKeyInfo()); }
  public Property getPackedTextures(GameProfile profile) {
    Collection<Property> c = profile.properties().asMap().get("textures");
    if (c == null) return null;
    for (Property p : c) if (p != null) return p;
    return null;
  }
  public String unpackTextures(Property p) {
    if (p == null) return "sin textura";
    String estado = p.signature() == null ? "UNSIGNED" : keys.stream().noneMatch(k -> k.validateProperty(p)) ? "INVALID" : "SIGNED";
    String json = new String(Base64.getDecoder().decode(p.value()));
    Matcher m = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
    while (m.find()) if (!disc.isAllowedTextureDomain(m.group(1))) return "ERROR Textures payload url is invalid: " + m.group(1);
    m.reset(); m.find();
    return estado + " " + m.group(1);
  }
  public static void main(String[] a) throws Exception {
    var s = new MinecraftServicesSessionService();
    // a[0]=valor a[1]=firma: perfil que llega del servidor con la skin apuntando al launcher de OTRA pc
    var m1 = com.google.common.collect.LinkedHashMultimap.<String, Property>create();
    m1.put("textures", new Property("textures", a[0], a[1]));
    var casos = new LinkedHashMap<String, GameProfile>();
    casos.put("ZyoCr0w(otra pc)", new GameProfile(UUID.randomUUID(), "ZyoCr0w", new PropertyMap(m1)));
    casos.put("Pepe(sin skin)", new GameProfile(UUID.randomUUID(), "Pepe", new PropertyMap(com.google.common.collect.LinkedHashMultimap.create())));
    casos.put("Nadie", new GameProfile(UUID.randomUUID(), "Nadie", new PropertyMap(com.google.common.collect.LinkedHashMultimap.create())));
    for (var e : casos.entrySet()) System.out.println(e.getKey() + " -> " + s.unpackTextures(s.getPackedTextures(e.getValue())));
    System.out.println("ZyoCr0w directo a unpackTextures -> " + s.unpackTextures(new Property("textures", a[0], a[1])));
    // estilo viejo
    var k = java.security.KeyPairGenerator.getInstance("RSA"); k.initialize(2048);
    System.out.println("isSignatureValid(firma NexLauncher, otra clave) = " + new Property("textures", a[0], a[1]).isSignatureValid(k.generateKeyPair().getPublic()));
    System.out.println("isSignatureValid(firma falsa) = " + new Property("textures", a[0], a[1].substring(4) + "AAAA").isSignatureValid(k.generateKeyPair().getPublic()));
    System.out.println("isWhitelistedDomain(nexskins) = " + TextureUrlChecker.isWhitelistedDomain("https://nexskins.bybrandonyt.workers.dev/textures/x"));
    System.out.println("isAllowedTextureDomain(evil.com) = " + s.disc.isAllowedTextureDomain("http://evil.com/x"));
  }
}
