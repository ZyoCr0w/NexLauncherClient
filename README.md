# NexClient

Mod PvP de [NexLauncher](https://github.com/ZyoCr0w/NexLauncherUpdates) para **Forge 1.8.9** y **Fabric 1.21.1**.

| Función | 1.8.9 | 1.21.1 |
|---|---|---|
| Teclas en pantalla (WASD, LMB/RMB con CPS, espacio) | ✓ | ✓ |
| Contador de CPS y de FPS | ✓ | ✓ |
| Editor para mover el HUD | ✓ | ✓ |
| Blockhit visual (pose de bloqueo de espada) | nativo | ✓ |
| Animación de blockhit estilo 1.7 | ✓ | ✓ |
| Sonido de blockhit (6 sonidos, volumen y tono) | ✓ | ✓ |
| Multiplicador de partículas (x1–x10, siempre crítico / filo) | ✓ | ✓ |

**Menú:** Shift derecho dentro del juego. Los ajustes se guardan en `.minecraft/config/nexclient.json`
(el mismo archivo para las dos versiones; NexLauncher también lo edita desde Ajustes → PvP).

La versión 1.21.1 necesita **Fabric API**.

## Compilar

GitHub Actions compila las dos versiones en cada push. Para publicar un release:

```
git tag v1.0.0
git push origin v1.0.0
```

En tu PC: `cd forge-1.8.9 && gradlew build` (Java 17) o `cd fabric-1.21.1 && gradlew build` (Java 25 para compilar; el mod funciona con Java 21).
El `.jar` queda en `build/libs/`.
