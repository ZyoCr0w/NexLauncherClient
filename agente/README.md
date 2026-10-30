# Agente de skins de NexLauncher

Java agent (`-javaagent`) que NexLauncher agrega al abrir el juego, con cuenta no premium (junto a
authlib-injector) y premium (solo). Cuando un servidor manda a un jugador sin skin, o con una skin que apunta
al launcher de otra PC (`http://127.0.0.1:otro-puerto`), authlib le pregunta a NexLauncher
(`-Dnexlauncher.skins`), que la busca en NexSkins (y si no, la de la cuenta premium con ese nombre).
Además hace que authlib acepte las skins firmadas con la clave común de NexLauncher y los dominios de
`-Dnexlauncher.dominios`, así un jugador premium también ve las skins de sus amigos no premium.
Funciona en cualquier versión de Minecraft (también 26.3+, con `com.mojang.authlib.services`), también Vanilla.

- `Agente` + `Parche`: editor mínimo de `.class` sin dependencias. Al principio de
  `YggdrasilMinecraftSessionService.getTextures` (authlib 1–3) y `getPackedTextures` (authlib 5+) agrega
  `aload_1; invokestatic Hooks.preparar; checkcast GameProfile; astore_1` y corre offsets, excepciones,
  tablas de líneas/variables y StackMapTable (incluidos los `Uninitialized`).
- Al principio de `Property.isSignatureValid`, `*.validateProperty(Property)` e
  `isWhitelistedDomain`/`isAllowedTextureDomain(String)` agrega
  `nop×3; aload_s; invokestatic Hooks.firma|dominio; ifeq +5; iconst_1; ireturn` con un marco nuevo
  (`same_frame`) donde sigue el código original; si el método no tenía StackMapTable, la crea.
- `Hooks` (jar aparte, va al cargador de arranque): devuelve el perfil con la skin de NexLauncher si el
  servidor no mandó ninguna. Solo usa clases de Java y reflexión; cualquier error deja el perfil igual.
- `prueba/Masiva2.java`: lo mismo con el cambio «aceptar» (métodos `(Objeto)Z`).
- `prueba/v3`: authlib de prueba con la forma de Minecraft 26.3 (`MinecraftServicesKeyInfo`, etc.).
- `prueba/Masiva.java`: aplica el parche a miles de clases reales (guava, commons-lang, groovy, kotlin) y
  las verifica con `-Xverify:all`.
- `nexskins-worker.js`: el servidor NexSkins (Cloudflare Workers + KV).

Compilar: `sh build.sh` (deja `build/nexskins-agente.jar` y `build/nexskins-hooks.jar`).
