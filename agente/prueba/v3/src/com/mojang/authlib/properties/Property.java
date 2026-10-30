package com.mojang.authlib.properties;
import java.security.*;
public record Property(String name, String value, String signature) {
  // estilo authlib 1.x–3.x (para probar ese parche también)
  public boolean isSignatureValid(PublicKey k) {
    try { Signature s = Signature.getInstance("SHA1withRSA"); s.initVerify(k); s.update(value.getBytes());
      return s.verify(java.util.Base64.getDecoder().decode(signature)); }
    catch (Exception e) { return false; }
  }
}
