package com.mojang.authlib.services;
public class TextureUrlChecker {
  // sin saltos: sin StackMapTable (como un método ya reescrito por authlib-injector)
  public static boolean isWhitelistedDomain(String url) { return Boolean.parseBoolean(System.getProperty("nunca." + url)); }
}
