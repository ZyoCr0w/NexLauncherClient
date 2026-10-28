package com.nexclient;

import java.util.ArrayDeque;

/** Clics por segundo: cuenta los clics del último segundo de cada botón del ratón. */
public final class Cps {
    private static final ArrayDeque<Long> IZQ = new ArrayDeque<Long>();
    private static final ArrayDeque<Long> DER = new ArrayDeque<Long>();

    private Cps() {
    }

    public static synchronized void clic(int boton) {
        if (boton == 0) {
            IZQ.addLast(System.currentTimeMillis());
        } else if (boton == 1) {
            DER.addLast(System.currentTimeMillis());
        }
    }

    public static synchronized int izquierdo() {
        return contar(IZQ);
    }

    public static synchronized int derecho() {
        return contar(DER);
    }

    private static int contar(ArrayDeque<Long> cola) {
        long limite = System.currentTimeMillis() - 1000;
        while (!cola.isEmpty() && cola.peekFirst() < limite) {
            cola.pollFirst();
        }
        return cola.size();
    }
}
