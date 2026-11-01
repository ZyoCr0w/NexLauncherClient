package com.nexclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lector de GIF animados en Java puro (sin AWT): cuadros completos en ARGB con su duración, respetando la
 * transparencia, los cuadros parciales, el entrelazado y cómo se borra cada cuadro. Mismo archivo en 1.8.9 y 1.21.1.
 */
public final class Gif {
    public final int ancho, alto;
    public final List<int[]> cuadros = new ArrayList<int[]>();
    public final List<Integer> tiempos = new ArrayList<Integer>();  // milisegundos de cada cuadro

    private Gif(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
    }

    /** Lee el GIF; como mucho {@code maximo} cuadros (los de más se saltan sumando su tiempo). null si no es válido. */
    public static Gif leer(byte[] b, int maximo) {
        try {
            return leer0(b, maximo);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int u16(byte[] b, int i) {
        return (b[i] & 255) | ((b[i + 1] & 255) << 8);
    }

    private static Gif leer0(byte[] b, int maximo) {
        if (b.length < 13 || b[0] != 'G' || b[1] != 'I' || b[2] != 'F') {
            return null;
        }
        int w = u16(b, 6), h = u16(b, 8);
        if (w <= 0 || h <= 0 || w > 1024 || h > 1024) {
            return null;
        }
        Gif g = new Gif(w, h);
        int i = 13;
        int[] global = null;
        if ((b[10] & 0x80) != 0) {
            int n = 1 << ((b[10] & 7) + 1);
            global = paleta(b, i, n);
            i += 3 * n;
        }
        int[] lienzo = new int[w * h];  // transparente
        int[] previo = null;
        int retraso = 100, transparente = -1, disposicion = 0;
        // primero se cuentan los cuadros para saber cuántos saltar
        List<int[]> todos = new ArrayList<int[]>();
        List<Integer> tiempos = new ArrayList<Integer>();
        while (i < b.length) {
            int t = b[i++] & 255;
            if (t == 0x3B) {
                break;
            }
            if (t == 0x21) {  // extensión
                int etiqueta = b[i++] & 255;
                if (etiqueta == 0xF9 && (b[i] & 255) >= 4) {  // control gráfico
                    int f = b[i + 1] & 255;
                    disposicion = (f >> 2) & 7;
                    retraso = u16(b, i + 2) * 10;
                    transparente = (f & 1) != 0 ? (b[i + 4] & 255) : -1;
                }
                while (i < b.length && b[i] != 0) {
                    i += (b[i] & 255) + 1;
                }
                i++;
                continue;
            }
            if (t != 0x2C) {
                break;
            }
            int fx = u16(b, i), fy = u16(b, i + 2), fw = u16(b, i + 4), fh = u16(b, i + 6);
            int f = b[i + 8] & 255;
            i += 9;
            int[] pal = global;
            if ((f & 0x80) != 0) {
                int n = 1 << ((f & 7) + 1);
                pal = paleta(b, i, n);
                i += 3 * n;
            }
            boolean entrelazado = (f & 0x40) != 0;
            int minimo = b[i++] & 255;
            // junta los subbloques de datos LZW
            int largo = 0, j = i;
            while (j < b.length && b[j] != 0) {
                largo += b[j] & 255;
                j += (b[j] & 255) + 1;
            }
            byte[] datos = new byte[largo];
            int k = 0;
            while (i < b.length && b[i] != 0) {
                int n = b[i] & 255;
                System.arraycopy(b, i + 1, datos, k, n);
                k += n;
                i += n + 1;
            }
            i++;
            byte[] indices = lzw(datos, minimo, fw * fh);
            if (disposicion == 3) {
                previo = lienzo.clone();
            }
            if (pal != null) {
                for (int p = 0; p < fw * fh; p++) {
                    int fila = p / fw, col = p % fw;
                    if (entrelazado) {
                        fila = filaEntrelazada(fila, fh);
                    }
                    int x = fx + col, y = fy + fila;
                    int idx = indices[p] & 255;
                    if (x < w && y < h && idx != transparente && idx < pal.length) {
                        lienzo[y * w + x] = pal[idx];
                    }
                }
            }
            todos.add(lienzo.clone());
            tiempos.add(retraso < 20 ? 100 : retraso);  // 0/10 ms en los navegadores es ~100 ms
            if (disposicion == 2) {  // se borra lo de este cuadro
                for (int y = fy; y < Math.min(h, fy + fh); y++) {
                    Arrays.fill(lienzo, y * w + fx, y * w + Math.min(w, fx + fw), 0);
                }
            } else if (disposicion == 3 && previo != null) {
                lienzo = previo;
            }
            retraso = 100;
            transparente = -1;
            disposicion = 0;
        }
        if (todos.isEmpty()) {
            return null;
        }
        // si hay demasiados cuadros, se toman parejos y cada uno suma el tiempo de los que se saltan
        int n = todos.size();
        int quedan = Math.min(n, Math.max(1, maximo));
        for (int q = 0; q < quedan; q++) {
            int desde = q * n / quedan, hasta = (q + 1) * n / quedan;
            int suma = 0;
            for (int x = desde; x < hasta; x++) {
                suma += tiempos.get(x);
            }
            g.cuadros.add(todos.get(desde));
            g.tiempos.add(suma);
        }
        return g;
    }

    private static int filaEntrelazada(int fila, int alto) {
        int[] inicio = {0, 4, 2, 1}, paso = {8, 8, 4, 2};
        int k = fila;
        for (int pasada = 0; pasada < 4; pasada++) {
            int filas = (alto - inicio[pasada] + paso[pasada] - 1) / paso[pasada];
            if (filas < 0) {
                filas = 0;
            }
            if (k < filas) {
                return inicio[pasada] + k * paso[pasada];
            }
            k -= filas;
        }
        return fila;
    }

    private static int[] paleta(byte[] b, int i, int n) {
        int[] p = new int[n];
        for (int k = 0; k < n; k++) {
            p[k] = 0xFF000000 | ((b[i + 3 * k] & 255) << 16) | ((b[i + 3 * k + 1] & 255) << 8) | (b[i + 3 * k + 2] & 255);
        }
        return p;
    }

    /** Descompresión LZW de GIF (códigos de largo variable, de 3 a 12 bits). */
    private static byte[] lzw(byte[] datos, int minimo, int total) {
        byte[] salida = new byte[total];
        int limpiar = 1 << minimo, fin = limpiar + 1;
        short[] prefijo = new short[4096];
        byte[] sufijo = new byte[4096];
        byte[] pila = new byte[4097];
        int disponible = limpiar + 2, largoCodigo = minimo + 1, mascara = (1 << largoCodigo) - 1;
        int anterior = -1, primero = 0;
        for (int c = 0; c < limpiar; c++) {
            sufijo[c] = (byte) c;
        }
        int bits = 0, acumulado = 0, pos = 0, o = 0;
        while (o < total) {
            while (bits < largoCodigo) {
                if (pos >= datos.length) {
                    return salida;  // datos cortos: lo que falte queda en 0
                }
                acumulado |= (datos[pos++] & 255) << bits;
                bits += 8;
            }
            int codigo = acumulado & mascara;
            acumulado >>= largoCodigo;
            bits -= largoCodigo;
            if (codigo == limpiar) {
                largoCodigo = minimo + 1;
                mascara = (1 << largoCodigo) - 1;
                disponible = limpiar + 2;
                anterior = -1;
                continue;
            }
            if (codigo == fin) {
                break;
            }
            if (anterior == -1) {
                salida[o++] = sufijo[codigo];
                anterior = codigo;
                primero = codigo;
                continue;
            }
            int entrada = codigo, top = 0;
            if (codigo >= disponible) {
                pila[top++] = (byte) primero;
                codigo = anterior;
            }
            while (codigo >= limpiar) {
                pila[top++] = sufijo[codigo];
                codigo = prefijo[codigo];
            }
            primero = sufijo[codigo] & 255;
            pila[top++] = (byte) primero;
            if (disponible < 4096) {
                prefijo[disponible] = (short) anterior;
                sufijo[disponible] = (byte) primero;
                disponible++;
                if ((disponible & mascara) == 0 && disponible < 4096) {
                    largoCodigo++;
                    mascara = (1 << largoCodigo) - 1;
                }
            }
            anterior = entrada;
            while (top > 0 && o < total) {
                salida[o++] = pila[--top];
            }
        }
        return salida;
    }
}
