package com.nexclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Textos de NexClient en inglés cuando Minecraft está en inglés (cualquier variante: en_us, en_gb…). Se traduce al
 * dibujar: los botones, el panel lateral, los títulos y las ayudas. «Texto: valor» se traduce por partes.
 * Este archivo es igual en 1.8.9 y en 1.21.1; cada versión dice cuál es el idioma del juego.
 */
public final class Tr {
    /** Código del idioma del juego (lo pone cada versión al iniciar). */
    public static java.util.concurrent.Callable<String> idioma;
    private static final Map<String, String> EN = new HashMap<String, String>();

    private Tr() {
    }

    private static void p(String es, String en) {
        EN.put(es, en);
    }

    static {
        String[] d = {
            "Ajustes", "Settings", "Listo", "Done", "HUD", "HUD", "Info", "Info", "Blockhit", "Blockhit", "Juego", "Game",
            "Visual", "Visual", "Mira", "Crosshair", "Mods", "Mods", "Sí", "Yes", "No", "No",
            "Activa lo que quieras ver y acomódalo con «Mover elementos del HUD».", "Turn on what you want to see and arrange it with “Edit HUD”.",
            "Al apuntar", "On target", "Al comer o beber, el brazo sigue golpeando como en 1.7 (sin sonido).", "While eating or drinking, the arm keeps swinging like in 1.7 (no sound).",
            "Alcance", "Reach", "Alternar R", "Toggle R", "Amarillo", "Yellow", "Apagada", "Off",
            "Apagado: solo suena al hacer blockhit.", "Off: it only plays when you blockhit.", "Aqua", "Aqua",
            "Arcoíris", "Rainbow", "Armadura", "Armor",
            "Arrastra: mover (Shift: libre) · Rueda o esquina: tamaño · Flechas: 1 px · Ctrl+Z", "Drag: move (Shift: free) · Wheel or corner: size · Arrows: 1 px · Ctrl+Z",
            "Arreglos y más FPS (trae OneConfig: Shift derecho)", "Fixes and more FPS (brings OneConfig: Right Shift)",
            "Así se ve en tu pantalla (vista previa al lado, más grande)", "This is how it looks on your screen (bigger preview beside it)",
            "Aumento inicial", "Starting zoom", "Azul", "Blue", "Barra espaciadora", "Space bar", "Blanco", "White",
            "Blockhit con comida", "Blockhit with food", "Blockhit visual", "Visual blockhit",
            "Blockhit: golpea y bloquea con la espada. «Estilo 1.7» mueve la espada al golpear bloqueando.", "Blockhit: hit and block with the sword. “1.7 style” swings the sword while blocking.",
            "Bloques por segundo.", "Blocks per second.", "Borde", "Outline", "Botones del ratón", "Mouse buttons",
            "CPS en las teclas", "CPS on the keys", "Cambia de color cuando apuntas a un jugador (elige el color abajo).", "Changes color when you aim at a player (pick the color below).",
            "Cambia el rojo con el que parpadean los jugadores al recibir daño.", "Changes the red flash players show when they take damage.",
            "Capas con movimiento", "Moving capes", "Capas de skin en 3D", "3D skin layers", "Clic derecho: guardar aquí", "Right-click: save here",
            "Clic: cargar", "Click: load", "Clic: pintar · Clic derecho: borrar (el centro está marcado)", "Click: paint · Right-click: erase (the center is marked)",
            "Color", "Color", "Color al apuntar", "Color on target", "Color de golpe", "Hit color", "Color del borde", "Outline color",
            "Color del contorno", "Block outline color", "Color propio al mirar un bloque.", "Your own color when looking at a block.",
            "Combo", "Combo", "Con espada y clic derecho, la espada se pone en pose de bloqueo como en 1.8.", "With a sword and right-click, the sword blocks like in 1.8.",
            "Contador de CPS", "CPS counter", "Contador de FPS", "FPS counter", "Contorno de bloque", "Block outline", "Coordenadas", "Coordinates",
            "Copiar código", "Copy code", "Corchetes", "Brackets", "Cosméticos", "Cosmetics", "Cruz", "Cross", "Cruz y punto", "Cross and dot",
            "Cuadrado", "Square", "Cuadrícula", "Grid", "Círculo", "Circle", "Círculo y punto", "Circle and dot",
            "Código copiado: pégalo a un amigo o en NexLauncher.", "Code copied: paste it to a friend or in NexLauncher.",
            "De todos", "Everyone's", "Deshacer (Ctrl+Z)", "Undo (Ctrl+Z)", "Destello al golpear", "Flash on hit", "Diamante", "Diamond",
            "Dinámica", "Dynamic", "Dirección (N, S…)", "Direction (N, S…)", "Distancia de tu último golpe.", "Distance of your last hit.",
            "Durabilidad de armadura y arma.", "Armor and weapon durability.", "Editar HUD", "Edit HUD", "El color va cambiando solo.", "The color keeps changing.",
            "En la lista de jugadores (Tab) ves el ping de cada uno en números.", "In the player list (Tab) you see everyone's ping as a number.",
            "En tercera persona", "In third person", "Escala", "Scale", "Escudo", "Shield", "Estilo 1.7", "1.7 style",
            "Etiquetas NexLauncher", "NexLauncher tags", "Experiencia", "Experience", "Flechas", "Arrows", "Fondo", "Background",
            "Fullbright", "Fullbright", "Golpe", "Hit", "Golpes seguidos sin que te peguen.", "Hits in a row without getting hit.",
            "Graba tus partidas", "Record your games", "Grosor", "Thickness", "Grosor del borde", "Outline thickness", "Hora", "Time", "Hora fija", "Fixed time",
            "La espada sigue golpeando mientras bloqueas (animación de 1.7).", "The sword keeps swinging while you block (1.7 animation).",
            "La línea encima del nombre de quienes juegan con NexLauncher.", "The line above the name of NexLauncher players.",
            "Lo copiado no es un código de mira (empieza con NXM:).", "What you copied isn't a crosshair code (it starts with NXM:).",
            "Mantener = spam", "Hold = spam", "Mantener R", "Hold R",
            "Mantener el clic izquierdo mientras bloqueas repite el golpe como si spamearas.", "Holding left-click while blocking repeats the hit as if you were spamming.",
            "Mantén Z; con la rueda del ratón acercas o alejas. La tecla se cambia en Controles.", "Hold Z; the mouse wheel zooms in or out. Change the key in Controls.",
            "Menú NexClient", "NexClient menu", "Menú principal y de pausa de NexClient (por encima del de cualquier paquete de texturas).", "NexClient main and pause menus (over any resource pack's).",
            "Metal", "Metal", "Mira cargada.", "Crosshair loaded.", "Mira personalizada", "Custom crosshair", "Mostrar", "Show",
            "Multiplicador", "Multiplier", "Multiplicar partículas", "Multiply particles", "Más partículas de crítico y de filo al golpear.", "More crit and sharpness particles when you hit.",
            "Naranja", "Orange", "Negro", "Black", "Ninguno", "None", "No dibuja entidades que no ves", "Doesn't draw entities you can't see",
            "No se ve mientras tensas el arco o la ballesta.", "Hidden while you draw a bow or crossbow.", "Ocultar con arco", "Hide with bow",
            "Ocultar escudo", "Hide shield", "Ocultar fuego", "Hide fire", "Ocultos", "Hidden", "Opacidad %", "Opacity %", "Pegar código", "Paste code",
            "Ping", "Ping", "Ping en el Tab", "Ping in Tab", "Pling", "Pling", "Pociones", "Potions", "Probar sonido", "Test sound", "Propia", "Custom", "Punto", "Dot",
            "Quita las llamas que tapan la pantalla.", "Removes the flames covering the screen.",
            "Quita «FPS», «CPS» y «ms»: se ven solo los números.", "Removes “FPS”, “CPS” and “ms”: only the numbers show.",
            "Reemplaza la mira del juego.", "Replaces the game's crosshair.", "Rehacer (Ctrl+Y)", "Redo (Ctrl+Y)", "Reloj", "Clock", "Reloj de 24 horas", "24-hour clock",
            "Restablecer", "Reset", "Restablecer todo", "Reset all", "Rojo", "Red", "Rosa", "Pink", "Se abre al correr o saltar y vuelve suave.", "Spreads when you sprint or jump and eases back.",
            "Se instalan o quitan la próxima vez que abras el perfil PvP desde NexLauncher.", "They're installed or removed the next time you open the PvP profile from NexLauncher.",
            "Separación", "Gap", "Shift derecho o Esc → NexClient abren este menú. Zoom: tecla Z (cámbiala en Controles).", "Right Shift or Esc → NexClient open this menu. Zoom: Z key (change it in Controls).",
            "Si bloqueas con un escudo en la otra mano, no se dibuja.", "If you block with a shield in the other hand, it isn't drawn.",
            "Siempre crítico", "Always crit", "Siempre despejado", "Always clear", "Siempre filo", "Always sharpness", "Sin lluvia en tu pantalla.", "No rain on your screen.",
            "Skins NexLauncher", "NexLauncher skins", "Solo la ves tú.", "Only you see it.", "Solo los míos", "Only mine", "Solo números (FPS, CPS, ms)", "Numbers only (FPS, CPS, ms)",
            "Solo visual.", "Visual only.", "Sombra", "Shadow", "Sonar en cada golpe", "Sound on every hit", "Sonido", "Sound", "Sonido de blockhit", "Blockhit sound",
            "Sprint / Sneak", "Sprint / Sneak", "Tamaño", "Size", "Tamaño real", "Real size", "Tecla G. Sin guía cerca, el elemento se pega a la cuadrícula.", "G key. With no guide nearby, the element snaps to the grid.",
            "Teclas", "Keys", "Teclas en pantalla", "Keystrokes", "Texto", "Text", "Todo vuelve a su lugar y tamaño de fábrica.", "Everything goes back to its default place and size.",
            "Toggle sneak", "Toggle sneak", "Toggle sprint", "Toggle sprint", "Tono", "Pitch", "Tu tecla de agacharse alterna.", "Your sneak key toggles.",
            "Tu tecla de correr alterna: pulsa una vez y sigues corriendo.", "Your sprint key toggles: press once and keep sprinting.",
            "Velocidad", "Speed", "Verde", "Green", "Ves todo iluminado, también cuevas.", "Everything is lit, caves too.", "Vista libre", "Freelook",
            "Volumen", "Volume", "WASD, clics y espacio en pantalla.", "WASD, clicks and space on screen.", "Zoom (tecla Z)", "Zoom (Z key)",
            "Zoom: mantén Z y usa la rueda para acercar o alejar. Vista libre: mantén R y mira alrededor.", "Zoom: hold Z and use the wheel to zoom in or out. Freelook: hold R and look around.",
            "Zoom", "Zoom", "«Color de golpe» cambia el rojo con el que parpadean al recibir daño.", "“Hit color” changes the red flash when players take damage.",
            "«Siempre crítico» y «Siempre filo» son solo visuales: no cambian el daño.", "“Always crit” and “Always sharpness” are visual only: they don't change damage.",
            "§lEditar HUD§r · posición, tamaño, fondo, borde y colores", "§lEdit HUD§r · position, size, background, outline and colors", "§lEditar mira…", "§lEdit crosshair…",
            "Editar mira…", "Edit crosshair…", "Cámara libre en tercera persona", "Third-person free camera",
            "CustomSkinLoader: ves las skins offline de NexLauncher en cualquier servidor", "CustomSkinLoader: see NexLauncher offline skins on any server",
            "Interfaz", "Interface", "Gris", "Gray", "Morado", "Purple", "Cian", "Cyan", "Lima", "Lime", "Dorado", "Gold",
        };
        for (int i = 0; i + 1 < d.length; i += 2) {
            p(d[i], d[i + 1]);
        }
    }

    public static boolean ingles() {
        try {
            String c = idioma == null ? null : idioma.call();
            return c != null && c.toLowerCase().startsWith("en");
        } catch (Exception e) {
            return false;
        }
    }

    /** El texto en el idioma del juego (en español, igual). */
    public static String t(String s) {
        if (s == null || s.isEmpty() || !ingles()) {
            return s;
        }
        String r = EN.get(s);
        if (r != null) {
            return r;
        }
        String limpio = s.trim();
        r = EN.get(limpio);
        if (r != null) {
            return s.replace(limpio, r);
        }
        int i = s.indexOf(": ");
        if (i > 0) {  // «Grosor: 2.0», «Teclas en pantalla: Sí»
            String a = s.substring(0, i), b = s.substring(i + 2);
            String ta = EN.get(a), tb = EN.get(b);
            if (ta != null || tb != null) {
                return (ta != null ? ta : a) + ": " + (tb != null ? tb : b);
            }
        }
        if (s.endsWith(": ") && EN.containsKey(s.substring(0, s.length() - 2))) {
            return EN.get(s.substring(0, s.length() - 2)) + ": ";
        }
        return s;
    }
}
