package nexlauncher.agente;
import java.io.*; import java.net.*; import java.util.*; import java.util.jar.*;
public class Masiva2 {
  public static void main(String[] a) throws Exception {
    int ok = 0, fallos = 0, verif = 0, otros = 0, metodos = 0;
    for (String ruta : a) {
      JarFile jf = new JarFile(ruta); URLClassLoader base = new URLClassLoader(new URL[]{new File(ruta).toURI().toURL()}, null);
      for (JarEntry je : Collections.list(jf.entries())) {
        String n = je.getName(); if (!n.endsWith(".class") || n.contains("module-info") || n.startsWith("META-INF")) continue;
        byte[] b = jf.getInputStream(je).readAllBytes();
        String cn = n.substring(0, n.length() - 6);
        // elige métodos de instancia cuyo primer parámetro es un objeto (no arreglo); checkcast a ese tipo
        final String[] tipo = {null};
        Parche.prueba = (nom, desc) -> false;
        Parche.pruebaAceptar = (nom, desc) -> !nom.startsWith("<") && !nom.equals("isSignatureValid") && desc.matches("\\(L[^;]+;\\)Z");
        byte[] nuevo;
        try { nuevo = Parche.aplicar(b); }
        catch (Exception e) { fallos++; System.out.println("PARSE " + cn + ": " + e); continue; }
        if (nuevo == null) continue;
        metodos++;
        final byte[] fb = nuevo; final String nombre = cn.replace('/', '.');
        final JarFile jfx = jf;
        ClassLoader cl = new ClassLoader(null) {
          protected Class<?> loadClass(String x, boolean r) throws ClassNotFoundException {
            if (x.startsWith("nexlauncher.")) return Masiva2.class.getClassLoader().loadClass(x);
            synchronized (this) {
              Class<?> c = findLoadedClass(x); if (c != null) return c;
              if (x.equals(nombre)) return defineClass(x, fb, 0, fb.length);
              JarEntry e2 = jfx.getJarEntry(x.replace('.', '/') + ".class");
              if (e2 == null) return super.loadClass(x, r);
              try { byte[] bb = jfx.getInputStream(e2).readAllBytes(); return defineClass(x, bb, 0, bb.length); } catch (IOException io) { throw new ClassNotFoundException(x); }
            }
          } };
        try { Class<?> c = cl.loadClass(nombre); java.lang.invoke.MethodHandles.lookup(); c.getDeclaredMethods(); Class.forName(nombre, true, cl); ok++; }
        catch (VerifyError | ClassFormatError e) { verif++; if (verif < 3) { System.out.println("VERIFY " + nombre + ": " + e.getMessage()); } }
        catch (Throwable e) { Throwable r = e; while (r.getCause() != null) r = r.getCause(); if (r instanceof VerifyError || r instanceof ClassFormatError) { verif++; if (verif < 6) System.out.println("VERIFY " + nombre + ": " + r); } else otros++; }
      }
    }
    System.out.println("clases cambiadas=" + metodos + " ok=" + ok + " errores_de_verificacion=" + verif + " otros(dependencias/inicio)=" + otros + " fallos_parse=" + fallos);
  }
}
