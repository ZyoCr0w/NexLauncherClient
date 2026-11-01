package com.nexclient;

import net.minecraft.client.player.AbstractClientPlayer;

/** Quién tiene en la mano el objeto que se está dibujando (el escudo no lo sabe por sí solo). */
public final class PortadorEscudo {
    private static final ThreadLocal<AbstractClientPlayer> ACTUAL = new ThreadLocal<AbstractClientPlayer>();

    private PortadorEscudo() {
    }

    public static AbstractClientPlayer actual() {
        return ACTUAL.get();
    }

    public static void poner(AbstractClientPlayer p) {
        ACTUAL.set(p);
    }
}
