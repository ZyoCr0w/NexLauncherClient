package nexlauncher.agente;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Editor mínimo de archivos .class (sin librerías): al principio de los métodos elegidos agrega
 *   aload_1; invokestatic Hooks.preparar(Object)Object; checkcast GameProfile; astore_1
 * (8 bytes: múltiplo de 4, así los tableswitch quedan alineados) y corre los offsets que dependen del código.
 */
final class Parche {
    private static final String[][] METODOS = {
            // authlib 1.x–3.x (Minecraft 1.7.6–1.20.1)
            {"getTextures", "(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;"},
            // authlib 5+ (Minecraft 1.20.2 en adelante)
            {"getPackedTextures", "(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/properties/Property;"},
    };
    private static final int N = 8;
    /** Solo para pruebas: elegir otros métodos (el checkcast usa el tipo del primer parámetro). */
    static java.util.function.BiPredicate<String, String> prueba;
    static String tipoPrueba;

    private Parche() {
    }

    static byte[] aplicar(byte[] c) throws IOException {
        CAMBIADOS.clear();
        ByteBuffer b = ByteBuffer.wrap(c);
        b.position(8);
        int nCp = b.getShort() & 0xffff;
        String[] utf = new String[nCp];
        for (int i = 1; i < nCp; i++) {
            int tag = b.get() & 0xff;
            switch (tag) {
                case 1: {
                    int len = b.getShort() & 0xffff;
                    byte[] s = new byte[len];
                    b.get(s);
                    utf[i] = new String(s, "UTF-8");  // los nombres que buscamos son ASCII
                    break;
                }
                case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                    b.position(b.position() + 4);
                    break;
                case 5: case 6:
                    b.position(b.position() + 8);
                    i++;
                    break;
                case 7: case 8: case 16: case 19: case 20:
                    b.position(b.position() + 2);
                    break;
                case 15:
                    b.position(b.position() + 3);
                    break;
                default:
                    throw new IOException("constante desconocida " + tag);
            }
        }
        int finCp = b.position();
        // Constantes nuevas al final del pool
        int k = nCp;
        int uHooks = k++, cHooks = k++, uNom = k++, uDesc = k++, nt = k++, mref = k++, uGp = k++, cGp = k++;
        ByteArrayOutputStream extra = new ByteArrayOutputStream();
        DataOutputStream e = new DataOutputStream(extra);
        e.writeByte(1); e.writeUTF("nexlauncher/agente/Hooks");
        e.writeByte(7); e.writeShort(uHooks);
        e.writeByte(1); e.writeUTF("preparar");
        e.writeByte(1); e.writeUTF("(Ljava/lang/Object;)Ljava/lang/Object;");
        e.writeByte(12); e.writeShort(uNom); e.writeShort(uDesc);
        e.writeByte(10); e.writeShort(cHooks); e.writeShort(nt);
        e.writeByte(1); e.writeUTF(tipoPrueba != null ? tipoPrueba : "com/mojang/authlib/GameProfile");
        e.writeByte(7); e.writeShort(uGp);
        if (k > 0xffff) {
            throw new IOException("pool lleno");
        }
        byte[] insercion = {0x2b, (byte) 0xb8, (byte) (mref >> 8), (byte) mref, (byte) 0xc0, (byte) (cGp >> 8), (byte) cGp, 0x4c};

        ByteArrayOutputStream out = new ByteArrayOutputStream(c.length + 256);
        DataOutputStream o = new DataOutputStream(out);
        o.write(c, 0, 8);
        o.writeShort(k);
        o.write(c, 10, finCp - 10);
        o.write(extra.toByteArray());
        // access, this, super, interfaces
        b.position(finCp);
        int ini = b.position();
        b.position(b.position() + 6);
        int nInt = b.getShort() & 0xffff;
        b.position(b.position() + 2 * nInt);
        // campos (sin cambios)
        int nCampos = b.getShort() & 0xffff;
        for (int i = 0; i < nCampos; i++) {
            b.position(b.position() + 6);
            saltarAtributos(b);
        }
        o.write(c, ini, b.position() - ini);
        int nMet = b.getShort() & 0xffff;
        o.writeShort(nMet);
        int cambiados = 0;
        for (int i = 0; i < nMet; i++) {
            int acceso = b.getShort() & 0xffff;
            int nom = b.getShort() & 0xffff;
            int desc = b.getShort() & 0xffff;
            boolean elegido = (acceso & 0x0008) == 0 && (prueba != null ? prueba.test(utf[nom], utf[desc])
                    : elegido(utf[nom], utf[desc]));
            o.writeShort(acceso);
            o.writeShort(nom);
            o.writeShort(desc);
            int nAtr = b.getShort() & 0xffff;
            o.writeShort(nAtr);
            for (int a = 0; a < nAtr; a++) {
                int an = b.getShort() & 0xffff;
                int largo = b.getInt();
                byte[] dato = new byte[largo];
                b.get(dato);
                if (elegido && "Code".equals(utf[an])) {
                    dato = codigo(dato, utf, insercion);
                    cambiados++;
                    CAMBIADOS.add(utf[nom]);
                }
                o.writeShort(an);
                o.writeInt(dato.length);
                o.write(dato);
            }
        }
        o.write(c, b.position(), c.length - b.position());  // atributos de la clase
        return cambiados == 0 ? null : out.toByteArray();
    }

    /** Nombres de los métodos cambiados en la última clase (para el registro del juego). */
    static final java.util.List<String> CAMBIADOS = new java.util.ArrayList<String>();

    private static boolean elegido(String nombre, String desc) {
        for (String[] m : METODOS) {
            if (m[0].equals(nombre) && m[1].equals(desc)) {
                return true;
            }
        }
        // versiones nuevas de authlib: el mismo método aunque cambie el tipo que devuelve o la clase
        return (nombre.equals("getTextures") || nombre.equals("getPackedTextures"))
                && desc.startsWith("(Lcom/mojang/authlib/GameProfile;");
    }

    private static void saltarAtributos(ByteBuffer b) {
        int n = b.getShort() & 0xffff;
        for (int i = 0; i < n; i++) {
            b.position(b.position() + 2);
            int largo = b.getInt();
            b.position(b.position() + largo);
        }
    }

    private static byte[] codigo(byte[] d, String[] utf, byte[] ins) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(d);
        int maxStack = b.getShort() & 0xffff;
        int maxLocals = b.getShort() & 0xffff;
        int largo = b.getInt();
        byte[] code = new byte[largo];
        b.get(code);
        ByteArrayOutputStream out = new ByteArrayOutputStream(d.length + 32);
        DataOutputStream o = new DataOutputStream(out);
        o.writeShort(Math.max(maxStack, 1));
        o.writeShort(Math.max(maxLocals, 2));
        o.writeInt(largo + N);
        o.write(ins);
        o.write(code);
        int nExc = b.getShort() & 0xffff;
        o.writeShort(nExc);
        for (int i = 0; i < nExc; i++) {
            o.writeShort((b.getShort() & 0xffff) + N);
            o.writeShort((b.getShort() & 0xffff) + N);
            o.writeShort((b.getShort() & 0xffff) + N);
            o.writeShort(b.getShort());
        }
        int nAtr = b.getShort() & 0xffff;
        ByteArrayOutputStream atrs = new ByteArrayOutputStream();
        DataOutputStream ao = o;
        o = new DataOutputStream(atrs);
        int nAtrQuitados = 0;
        for (int a = 0; a < nAtr; a++) {
            int an = b.getShort() & 0xffff;
            int l = b.getInt();
            byte[] x = new byte[l];
            b.get(x);
            String nombre = utf[an];
            if ("LineNumberTable".equals(nombre)) {
                ByteBuffer y = ByteBuffer.wrap(x);
                int n = y.getShort() & 0xffff;
                for (int i = 0; i < n; i++) {
                    int p = y.position();
                    y.putShort(p, (short) ((y.getShort(p) & 0xffff) + N));
                    y.position(p + 4);
                }
            } else if ("LocalVariableTable".equals(nombre) || "LocalVariableTypeTable".equals(nombre)) {
                ByteBuffer y = ByteBuffer.wrap(x);
                int n = y.getShort() & 0xffff;
                for (int i = 0; i < n; i++) {
                    int p = y.position();
                    int ini = y.getShort(p) & 0xffff;
                    if (ini == 0) {
                        y.putShort(p + 2, (short) ((y.getShort(p + 2) & 0xffff) + N));
                    } else {
                        y.putShort(p, (short) (ini + N));
                    }
                    y.position(p + 10);
                }
            } else if ("StackMapTable".equals(nombre)) {
                x = marcos(x);
            } else if (nombre != null && nombre.contains("TypeAnnotations")) {
                nAtrQuitados++;  // apuntan a offsets del código; no se usan para jugar
                continue;
            }
            o.writeShort(an);
            o.writeInt(x.length);
            o.write(x);
        }
        ao.writeShort(nAtr - nAtrQuitados);
        ao.write(atrs.toByteArray());
        return out.toByteArray();
    }

    /** El primer marco cuenta su desplazamiento desde el inicio; los Uninitialized(offset) apuntan a un «new». */
    private static byte[] marcos(byte[] x) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(x);
        int n = b.getShort() & 0xffff;
        ByteArrayOutputStream out = new ByteArrayOutputStream(x.length + 4);
        DataOutputStream o = new DataOutputStream(out);
        o.writeShort(n);
        for (int i = 0; i < n; i++) {
            int t = b.get() & 0xff;
            int extra = i == 0 ? N : 0;
            if (t < 64) {
                escribirDelta(o, t + extra, 0, 251);
            } else if (t < 128) {
                escribirDelta(o, t - 64 + extra, 64, 247);
                tipo(b, o);
            } else if (t == 247) {
                o.writeByte(247);
                o.writeShort((b.getShort() & 0xffff) + extra);
                tipo(b, o);
            } else if (t >= 248 && t <= 251) {
                o.writeByte(t);
                o.writeShort((b.getShort() & 0xffff) + extra);
            } else if (t >= 252 && t <= 254) {
                o.writeByte(t);
                o.writeShort((b.getShort() & 0xffff) + extra);
                for (int k = 0; k < t - 251; k++) {
                    tipo(b, o);
                }
            } else if (t == 255) {
                o.writeByte(t);
                o.writeShort((b.getShort() & 0xffff) + extra);
                for (int parte = 0; parte < 2; parte++) {  // locales y pila
                    int m = b.getShort() & 0xffff;
                    o.writeShort(m);
                    for (int k = 0; k < m; k++) {
                        tipo(b, o);
                    }
                }
            } else {
                throw new IOException("marco reservado " + t);
            }
        }
        return out.toByteArray();
    }

    /** SAME (base 0, extendido 251) o SAME_LOCALS_1_STACK_ITEM (base 64, extendido 247). */
    private static void escribirDelta(DataOutputStream o, int delta, int base, int extendido) throws IOException {
        if (delta < 64) {
            o.writeByte(base + delta);
        } else {
            o.writeByte(extendido);
            o.writeShort(delta);
        }
    }

    private static void tipo(ByteBuffer b, DataOutputStream o) throws IOException {
        int tag = b.get() & 0xff;
        o.writeByte(tag);
        if (tag == 7) {
            o.writeShort(b.getShort());
        } else if (tag == 8) {
            o.writeShort((b.getShort() & 0xffff) + N);  // Uninitialized: offset del «new»
        } else if (tag > 8) {
            throw new IOException("tipo de verificación desconocido " + tag);
        }
    }
}
