package com.mojang.authlib.services;
import com.mojang.authlib.properties.Property; import java.security.*;
public class MinecraftServicesKeyInfo {
  final PublicKey key;
  public MinecraftServicesKeyInfo() throws Exception { KeyPairGenerator g = KeyPairGenerator.getInstance("RSA"); g.initialize(4096); key = g.generateKeyPair().getPublic(); }
  public boolean validateProperty(Property p) {
    Signature s;
    try { s = Signature.getInstance("SHA1withRSA"); s.initVerify(key); s.update(p.value().getBytes()); }
    catch (Exception e) { throw new RuntimeException(e); }
    try { return s.verify(java.util.Base64.getDecoder().decode(p.signature())); }
    catch (SignatureException e) { System.out.println("ERROR Failed to verify signature on property " + e.getMessage()); return false; }
  }
}
