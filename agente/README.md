# Agente de skins de NexLauncher

Java agent (`-javaagent`) que NexLauncher agrega al abrir el juego con una cuenta no premium. Cuando un
servidor manda a un jugador sin skin, authlib le pregunta a NexLauncher (`-Dnexlauncher.skins`), que la busca
en NexSkins. Funciona en cualquier versión de Minecraft, también Vanilla.

- `Agente` + `Parche`: editor mínimo de `.class` sin dependencias. Al principio de
  `YggdrasilMinecraftSessionService.getTextures` (authlib 1–3) y `getPackedTextures` (authlib 5+) agrega
  `aload_1; invokestatic Hooks.preparar; checkcast GameProfile; astore_1` y corre offsets, excepciones,
  tablas de líneas/variables y StackMapTable (incluidos los `Uninitialized`).
- `Hooks` (jar aparte, va al cargador de arranque): devuelve el perfil con la skin de NexLauncher si el
  servidor no mandó ninguna. Solo usa clases de Java y reflexión; cualquier error deja el perfil igual.
- `prueba/Masiva.java`: aplica el parche a miles de clases reales (guava, commons-lang, groovy, kotlin) y
  las verifica con `-Xverify:all`.
- `nexskins-worker.js`: el servidor NexSkins (Cloudflare Workers + KV).

Compilar:

```
javac --release 8 -d a src/nexlauncher/agente/Agente.java src/nexlauncher/agente/Parche.java
javac --release 8 -d h src/nexlauncher/agente/Hooks.java
printf 'Premain-Class: nexlauncher.agente.Agente\n' > M.MF
(cd a && jar cfm ../nexskins-agente.jar ../M.MF nexlauncher) && (cd h && jar cf ../nexskins-hooks.jar nexlauncher)
```
