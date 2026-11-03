# Port de Tribulation a Minecraft 26.3 (Fabric): plan y checklist

Documento pensado para dos usos: (1) revisar qué necesitás instalar en la Mac, y (2) importarlo como contexto en Claude Code dentro del fork (podés copiarlo como `PORTING.md` en la raíz del repo, o pegar la sección "Prompt inicial").

> Estado de la información: verificado el 2026-11-01 en las páginas de Modrinth, CurseForge y GitHub del mod. Lo que no pude comprobar está marcado como **[no verificado]**.

---

## 1. Objetivo

Hacer un fork de `rfizzle/tribulation` (mod de dificultad progresiva, Fabric, hoy solo **1.21.1**) y portarlo a **Minecraft 26.3** para usarlo en un server survival con amigos.

Criterio de éxito, en orden:
1. El fork compila en 26.3 y `./gradlew test` pasa.
2. `./gradlew runGametest` pasa.
3. El server dedicado arranca y el escalado de mobs funciona (comandos `/tribulation ...`).
4. Un cliente se conecta, ve el HUD y los ítems funcionan.
5. Efectos visuales (Blood Moon, oscuridad de noche) funcionan. **Este paso es opcional**: si se complica, se desactiva y el mod sigue siendo usable.

## 2. Datos del proyecto original (verificados)

| Dato | Valor |
|---|---|
| Repo | https://github.com/rfizzle/tribulation |
| Licencia | MIT (se puede forkear; hay que conservar `LICENSE` y el copyright) |
| Versión actual | v1.1.0 (14 de octubre de 2026) |
| Minecraft soportado | solo 1.21.1 |
| Loader / Java hoy | Fabric Loader 0.16.10+, Java 21 |
| Dependencias duras | Solo Fabric API |
| Dependencias opcionales | Mod Menu, Cloth Config, Jade/WTHIT, EMI/REI/JEI, Meridian |
| Instalación | Requerido en **cliente y servidor** (agrega ítems, bloques y payloads de red) |
| Pull requests | Solo colaboradores: por eso el fork es el camino |
| Tests | `./gradlew test` (unitarios) y `./gradlew runGametest` (gametests de Fabric) |
| Docs internas | `AGENTS.md`, `CLAUDE.md`, `design/`, `.ai/`, `.claude/`, `changelogs/` |
| Actividad | Último release hace pocas semanas; sin issues ni planes de 26.x publicados |
| Comunidad | Pocas descargas (CurseForge ~501, Modrinth ~908); no encontré reviews |

## 3. Destino: 26.3

| Dato | Valor |
|---|---|
| Minecraft | 26.3 ("Wilderness Bound", salió el 15 de septiembre de 2026) |
| Java | **25** |
| Fabric Loader | 0.19.5 (estable para 26.3) |
| Fabric API | `0.161.0+26.3` o más nueva (el sufijo `+26.3` es obligatorio) |
| Aviso importante | En 26.3 se eliminaron varias interfaces de Fabric API que usaban mods viejos; los jars de 26.2 pueden no arrancar |

Referencia útil: otros ports 1.21.1 → 26.x documentan migrar "nombres de la era Yarn" a la API nueva y subir a Java 25. No asumas que la configuración de `build.gradle` de 1.21.1 sirve: verificá la de Fabric/Loom para 26.x.

---

## 4. Qué necesitás en la Mac

### Imprescindible
- [ ] **JDK 25** con build para Apple Silicon (arm64). Comprobá con `java -version`. Si ya corrés tu server en 26.3 en esa Mac, probablemente ya lo tenés.
- [ ] **Git** y una cuenta de **GitHub** (para el fork). `gh` (GitHub CLI) es cómodo pero opcional.
- [ ] **IntelliJ IDEA** (Community alcanza) con el JDK 25 configurado como SDK del proyecto.
- [ ] **Claude Code** (o el asistente que uses) corriendo dentro de la carpeta del fork.
- [ ] Conexión a internet: Gradle baja Minecraft, Fabric Loom y las dependencias.
- [ ] ~10 GB libres en disco (Gradle, caches de Minecraft y mundos de prueba).

### Recomendado
- [ ] **Una carpeta y un puerto de pruebas aparte** del server real (por ejemplo `~/mc-test-server`, puerto 25566), para no tocar el mundo de tus amigos.
- [ ] **Backup** del mundo real antes de instalar cualquier mod nuevo.
- [ ] **Prism Launcher** (o el launcher que uses) con una instancia "Tribulation-dev" de 26.3 + Fabric, para probar el jar final como cliente real.
- [ ] Mod Menu y Cloth Config de 26.3 (existen: Mod Menu `21.0.0`, Cloth Config `26.3.159`, ver «Integraciones opcionales» en la sección 14); el mod funciona sin ellos.

### Cosas a tener en cuenta (Mac Air)
- El MacBook Air no tiene ventilador: compilar + gametests + cliente de desarrollo a la vez puede hacer que se throttlee. Hacelo por etapas.
- Si esa misma Mac sirve el survival, **no corras el entorno de desarrollo mientras tus amigos juegan**.
- Mantené el jar final para tu PC con Prism: compilás en la Mac y copiás el `.jar` de `build/libs/`.

---

## 5. Pasos de arranque

```bash
# 1. Fork en GitHub (web o CLI) y clonar el fork
gh repo fork rfizzle/tribulation --clone
cd tribulation

# 2. Verificar herramientas
java -version
git --version

# 3. Línea base: ANTES de tocar nada, probar que el proyecto original compila
#    (usará su propia versión de Java; si pide Java 21, instalá uno extra solo para este paso)
./gradlew build
./gradlew test
```

La línea base es importante: si el proyecto original no compila en tu Mac, el problema es del entorno y no del port.

---

## 6. Plan por fases (un commit por fase)

**Fase 0: Línea base.** Build y tests del original en 1.21.1. Anotar resultados.

**Fase 1: Quitar lo opcional que complique.**
- Desactivar o sacar del build las integraciones opcionales: Mod Menu, Cloth Config, Jade/WTHIT, EMI/REI/JEI, Meridian. Se pueden reponer después si existen para 26.3.

**Fase 2: Archivos de build.**
- Subir a Java 25, Fabric Loader 0.19.5+ y Fabric API `+26.3`.
- Revisar `build.gradle`, `gradle.properties`, `versions-common.properties`, `fabric.mod.json` (versiones de dependencias y `minecraft`).
- Revisar cómo declara Loom las dependencias de Minecraft en 26.x.

**Fase 3: Compilar y arreglar errores.**
- Resolver por paquetes, empezando por la lógica común (escalado, abilities, config) y dejando el cliente para después.
- No cambiar comportamiento: solo adaptar APIs.

**Fase 4: Tests.**
- `./gradlew test` hasta que pase. **No borrar ni debilitar tests** para que pasen: si un test falla, entender si es un cambio real de API o un bug del port.

**Fase 5: Gametests.**
- `./gradlew runGametest`.

**Fase 6: Servidor dedicado de pruebas.**
- Arrancar el server, revisar logs completos (los mixins que no aplican suelen fallar acá, no en la compilación).
- Probar con la checklist de la sección 8.

**Fase 7: Cliente.**
- HUD del nivel, payloads de red (sincronización de config y nivel), keybind de "Peek Tier Detail".
- Si el render no compila o crashea: desactivar temporalmente (`enableTierHud=false`) y seguir.

**Fase 8: Efectos visuales (opcional).**
- Blood Moon: cielo, niebla y tinte de la luna.
- Oscuridad de noche ("Oppressive Nights", opt-in).
- Partículas de amenaza.

**Fase 9: Empaquetar y probar en el cliente real.**
- `./gradlew build` → jar en `build/libs/` → instalar en el server y en tu instancia de Prism.

---

## 7. Zonas de riesgo (qué suele romperse)

| Área | Por qué es riesgosa |
|---|---|
| Mixins (p. ej. el de Totem of Undying) | Compilan pero pueden fallar al aplicarse; revisar logs de arranque |
| Payloads de red (`TribulationLevelPayload` y sync de config) | Los codecs/registro de paquetes cambian entre versiones |
| HUD y peek panel | El pipeline de render cambió en 26.2; es cliente-only |
| Blood Moon visual, niebla, luz nocturna | Código de render; la parte más difícil |
| Ítems custom (Heart Fragment, Shatter Shard, Ascendant Shard) | Registro de ítems, componentes, recetas y advancements |
| Spawn caps y grupos de spawn | Dependen de APIs de spawning que pueden haber cambiado |
| Equipamiento y encantamientos de mobs | Los encantamientos son data-driven en versiones modernas |
| Raids, patrullas y trial spawners | Escalado atado a mecánicas vanilla que cambian |
| Tags (`#c:soulbound`, `c:bosses`) | Verificar que sigan existiendo las convenciones |
| Datos guardados por mundo y jugador | Formato NBT/codecs: probar con un mundo nuevo, no con el real |

---

## 8. Checklist de prueba manual (en el server de pruebas)

Comandos del mod (el original los tiene todos):
- [x] `/tribulation info`: ver nivel, tier y progreso.
- [x] `/tribulation set <jugador> 250`: llevar a tier 5 para ver todas las habilidades.
- [x] `/tribulation inspect` y `/tribulation debug <jugador>`: ver el desglose de escalado de un mob.
- [x] `/tribulation reload`: recargar `config/tribulation.json`.
- [x] `/tribulation bloodmoon start` y `stop`.

Comportamiento:
- [x] Spawnear zombie, esqueleto, araña y creeper: verificar vida/daño y habilidades por tier.
- [x] Equipamiento de mobs por tier.
- [x] Muerte → Death Relief (pierde 2 niveles).
- [x] Craftear y usar Shatter Shard y Ascendant Shard.
- [x] Heart Fragment (si activás Hardcore Hearts).
- [x] Élites (nivel 50+): afijos y drops.
- [x] Pack Tactics (tier 3+).
- [x] Jefes (Dragon/Wither): escalado más suave (probado con el Wither).
- [x] Reinicio del server: el nivel debe persistir.
- [x] Con 2 jugadores: modos de multijugador (nearest / average / max). Probado el 2026-11-02 (ver «Multijugador con 2 jugadores» en la sección 14).

Cliente:
- [x] HUD visible y correcto; panel de detalle con Left Alt.
- [x] Config sincronizada desde el server al conectarse.

---

## 9. Reglas para la IA (copiar a `AGENTS.md`/`CLAUDE.md` del fork)

- No cambiar mecánicas de juego ni balance: este es un port, no una versión nueva.
- Un commit por fase, con mensaje claro.
- Si algo se desactiva temporalmente (HUD, Blood Moon visual), anotarlo en `PORTING.md` con el motivo.
- Nunca borrar o relajar tests para que pasen.
- Después de cada fase: `./gradlew build` y `./gradlew test`.
- Ante dudas de API en 26.x, consultar la documentación oficial de Fabric y el código fuente de Minecraft de esa versión; **no inventar nombres de clases o métodos**.
- Si un mixin no aplica, explicar por qué (qué cambió en la clase objetivo).

---

## 10. Prompt inicial (para pegar en Claude Code, dentro del fork)

```text
Este repo es un fork de rfizzle/tribulation (mod de Fabric para Minecraft 1.21.1, licencia MIT).
Quiero portarlo a Minecraft 26.3 con Java 25, Fabric Loader 0.19.5+ y Fabric API 0.161.0+26.3.

Lee primero AGENTS.md, CLAUDE.md, design/ y PORTING.md.
Trabaja por fases (están en PORTING.md), un commit por fase.
Empieza por la Fase 0 (build y tests del original) y reporta los resultados antes de modificar nada.
No cambies gameplay ni balance. No elimines ni debilites tests.
Si no estás seguro de un nombre de API de 26.x, dilo y verifícalo en la documentación o en el código fuente de Minecraft, no lo adivines.
```

---

## 11. Publicación y licencia (solo si lo vas a compartir)

- Mantené `LICENSE` (MIT) y el copyright original de rfizzle.
- Si lo publicás, **cambiá el nombre y el mod id** para no pisar al original en Modrinth/CurseForge.
- Para uso privado en tu server no necesitás publicar nada.
- Abrile un issue al autor preguntando si piensa portar a 26.x: si ya lo está haciendo, te ahorra el trabajo.

## 12. Plan B

Si el port se alarga, **BuffMobs** ya soporta 26.3 en Fabric (escalado por días, élites, rasgos, Blood Moon, dificultad adaptativa) y se puede usar mientras tanto. Alternativas: Hostile Mobs Improve Over Time y Progressive World Difficulty.

## 13. Qué no pude verificar

- La fecha del último commit del repo (GitHub bloqueó el acceso automatizado).
- El contenido de los 2 comentarios en CurseForge.
- ~~Si existen versiones 26.3 de Mod Menu, Cloth Config, Jade y EMI/REI.~~ Verificado el 2026-11-01: existen todas menos EMI y WTHIT (ver «Integraciones opcionales» en la sección 14).
- Cuánto tiempo va a llevar el port: depende de cuánto cambió el render y de cuántos mixins fallen.

---

## 14. Estado del port

- Rama de trabajo: `mc/26.3`. `master` queda como espejo de `rfizzle/tribulation` (remoto `upstream`).
- Issue upstream: https://github.com/rfizzle/tribulation/issues/265
- No correr `make sync`: pisaría los archivos de build adaptados a 26.3.

### Línea base (Fase 0, 1.21.1, JDK 21)

- `./gradlew build`: OK. 896 tests unitarios en 57 clases, 0 fallos.
- `./gradlew runGametest`: OK. 147 gametests obligatorios.
- Warnings de mixin: `BloodMoonFogMixin.java:27` (`RenderSystem.clearColor`) y `BloodMoonMoonTintMixin.java:25` (`RenderSystem.setShaderColor`) no encuentran mapping. Ya existían en el original.

### Fase 3 (compilación, 2026-11-01)

- Los cuatro source sets (`main`, `client`, `test`, `gametest`) compilan en 26.3 con JDK 25. Detalle de cada reemplazo de API en `odd/tasks/port-26-3.md` (L11).
- Pendiente de verificar en fases siguientes: los `SavedData` ahora se guardan en `dimensions/minecraft/overworld/data/tribulation/` (ruta de 26.x), así que un mundo actualizado desde 1.21.1 arrancaría sin el estado de Tribulation (resuelto en la Fase 6 con una migración); faltan las definiciones `assets/tribulation/items/*.json` que pide 26.x para el modelo de cada ítem (Fase 7); hay que regenerar datagen por el cambio de id del fondo de advancements (Fase 4).

### Fase 4 (tests unitarios, 2026-11-01)

- `./gradlew build`: OK. 898 tests unitarios, 0 fallos (los 896 de la línea base más 2 nuevos para Meridian).
- `gradle/concord-common.gradle`: se quitó la exclusión de `fabric-api` en `testRuntimeClasspath`. Existía porque en 1.21.1 Loom dejaba una copia sin remapear de fabric-api cuyo access widener chocaba con la remapeada. En 26.x no hay remapeo, así que la exclusión sacaba fabric-api entero y `fabric-loader-junit` no arrancaba ("requires any version of fabric-api, which is missing").
- Mixins que no aplicaban (javac no valida los strings de `method`/`target`, por eso la Fase 3 no los vio). Se auditaron los 25 mixins de `tribulation.mixins.json` con `javap` contra el jar de 26.3, y el nuevo `RaidsMixin` se verificó cargando `Raids` en un test descartable:
  - `MobAbilityMixin` y `HuskAbilityMixin`: `doHurtTarget(Entity)` pasó a ser `doHurtTarget(ServerLevel, Entity)`. El Husk sigue aplicando Hunger I antes del `RETURN` donde lo subimos a Hunger II.
  - `NaturalSpawnerSpawnStateMixin`: `SpawnState.canSpawnForCategory` se dividió en `canSpawnForCategoryGlobal` (el tope global con `getMaxInstancesPerChunk`, que es lo que redirigimos) y `canSpawnForCategoryLocal` (que ya cubre `LocalMobCapCalculatorMixin`). Mismo cálculo.
  - `RaidScalingMixin`: `Raid` ya no guarda su `ServerLevel` y el constructor `Raid(BlockPos, Difficulty)` no lo recibe, pero ahí es donde corre la primera llamada a `getNumGroups`, que captura el tier. El nuevo `RaidsMixin` envuelve el `new Raid(...)` de `Raids.getOrCreateRaid` (el único lugar que crea raids vivos) y publica el nivel en `scaling/RaidCreationContext` (un `ThreadLocal` que se limpia en `finally`) mientras corre el constructor.
- Meridian: `MeridianEquipmentCompatApiTest` prueba que los `MethodHandle` resuelven y que los valores pasan bien por los casts de `invoke`, contra stand-ins con la forma de `meridian-1.0.0` en `src/test/java/com/rfizzle/meridian/api/`. Se verificó que falla si se cambia la firma de un handle.
- Datagen regenerado: solo cambian las 11 advancements (`background` ahora es `minecraft:gui/advancements/backgrounds/stone` y el ícono omite `count: 1`, que es el valor por defecto en 26.x).

### Fase 5 (gametests, 2026-11-01)

- `./gradlew runGametest`: OK. 149 gametests (los 148 que ya existían más uno nuevo de raids), 0 fallos. `./gradlew build`: OK, 898 tests unitarios.
- El server no arrancaba: las dos recetas escritas a mano (`ascendant_shard`, `heart_fragment`) y sus advancements seguían en formato 1.21.1. En 26.x un ingrediente es un id (`"S": "tribulation:shatter_shard"`, ya no `{"item": ...}`) y la condición de `recipe_unlocked` se llama `recipes`. Un server dedicado también habría fallado al cargar el mundo.
- Cambios de 26.x que afectaban a los tests, no al mod:
  - El framework encierra cada estructura de test en barreras, techo incluido, salvo que el test declare `skyAccess = true`. En una estructura de 3 de alto, la barrera queda encima de la cama (vanilla la da por obstruida) y a la altura de los ojos de los mobs (corta la línea de visión que pide Pack Tactics). Los tests de cama de Blood Moon y los siete de alerta de Pack Tactics usan `skyAccess = true`; sin eso, los cinco tests negativos de Pack Tactics pasaban sin probar nada.
  - `CompoundTag.getString` devuelve `Optional<String>`: `"deadeye".equals(...)` compilaba pero siempre daba falso. Se usa `getStringOr`.
  - `ServerPlayer.spawnInvulnerableTime` ya no existe: un jugador es invulnerable hasta que su cliente avisa que cargó (`connection.hasClientLoaded()`). El test manda el mismo paquete que un cliente real (`ServerboundPlayerLoadedPacket`).
  - `Mob.setTarget` y `getTarget` ahora descartan jugadores en creativo, y `MockPlayers` forzaba `isCreative() == true` aunque el test pusiera supervivencia. Se quitó ese override: el server de tests ya pone a los jugadores en creativo por defecto, así que solo cambia algo para los tests que piden supervivencia a propósito. `MockPlayers` es un archivo de Concord: este cambio hay que llevarlo al hub (igual que el de `level().getServer()` de la Fase 3).
- Nuevo gametest `raidWaveCountFollowsTargetedPlayerTier`: crea raids reales con `Raids.createOrExtendRaid` y lee el `group_count` guardado en el raid. Con tier 0 da las olas de vanilla, con tier 5 suma las extra, y con el escalado apagado vuelve a vanilla. Se comprobó que falla sin `RaidsMixin` (da 5 en vez de 7).

### Fase 6 (servidor dedicado, 2026-11-01)

- `./gradlew runServer` con un mundo nuevo: arranca en ~1 s, sin warnings ni errores de mixins.
- Desde la consola funcionan `/tribulation config`, `reload`, `bloodmoon` y `bloodmoon start`/`stop` (de noche), y la Blood Moon termina sola al amanecer. Una Blood Moon iniciada de día se corta en el tick siguiente: es la misma lógica que en `master` (`shouldEnd`), no un cambio del port. Una Blood Moon activa sobrevive al reinicio del server.
- `info`, `set`, `inspect`, `debug`, `hearts` e `inventory` piden un jugador conectado ("A player is required"): quedan para la Fase 9 con el cliente real.
- **Migración de datos guardados.** En 26.x el almacenamiento del overworld guarda cada `SavedData` en `dimensions/minecraft/overworld/data/<namespace>/<nombre>.dat`. La actualización de mundos de vanilla (`DimensionStorageFileFix`) solo mueve los archivos de vanilla que nombra, así que `data/tribulation_players.dat` y `data/tribulation_blood_moon.dat` quedaban donde el mod ya no los lee: en un mundo actualizado todos los jugadores volvían a nivel 0. `data/LegacySavedDataMigration` mueve cada archivo a la ruta nueva en `SERVER_STARTING` (antes de que se carguen los mundos), sin convertir nada porque el NBT no cambió. Nunca pisa un archivo que ya exista en la ruta nueva (avisa en el log y deja el viejo intacto) y, si el movimiento falla, deja el archivo viejo en su lugar y explica cómo moverlo a mano.
- Probado de punta a punta con un mundo creado en 1.21.1 (`master`, JDK 21) con una Blood Moon activa: sin la migración, 26.3 lo abre con la Blood Moon `inactive`; con la migración, el log dice `Moved legacy Tribulation data ...` y sigue `ACTIVE`. Si el mundo ya se había abierto una vez sin migración (existen los dos archivos), avisa y no toca nada.
- `./gradlew build`: OK, 904 tests unitarios (898 + 6 de la migración). `./gradlew runGametest`: OK, 149 gametests.
- Después de la review, cuatro arreglos:
  - **Raids cargados desde disco**: en 1.21.1, un raid restaurado calculaba sus olas extra en la primera ola con ravager, dentro de `spawnGroup`, leyendo `Raid.level`. En 26.x no había nivel ahí y quedaba en 0 olas extra. `RaidScalingMixin` ahora publica el nivel mientras corre `spawnGroup`. Nuevo gametest `restoredRaidKeepsTierWavesWhenSpawningAGroup` (falla sin el arreglo: 5 en vez de 7).
  - **Día de la Blood Moon**: sin cambios. `DayTimeToClockFix` de vanilla copia el `DayTime` de 1.21.1 al reloj `minecraft:overworld`, que es lo que devuelve `getOverworldClockTime()`, así que el `LastRolledDay` guardado sigue valiendo.
  - **UUIDs de 1.21.1**: dos tests nuevos prueban que un jugador guardado con `putUUID` (array de 4 ints) se carga con todos sus campos y que las entradas rotas se saltean.
  - **Mensajes de la migración**: si falla, o si ya existe el archivo nuevo, el log ahora dice que hay que reemplazar el archivo nuevo con el viejo.
- `./gradlew build`: OK, 907 tests unitarios. `./gradlew runGametest`: OK, 150 gametests.

### Fase 7 (cliente, 2026-11-01)

- Faltaban las definiciones de modelo de los tres ítems (`assets/tribulation/items/*.json`, obligatorias desde 1.21.4): sin ellas, el ítem se dibuja con la textura de "falta textura" (cuadros violeta y negro). Las protege `ItemModelResourceContractTest`, que revisa la cadena completa: definición, modelo y textura.
- Nuevo client gametest (`src/clientGametest`), que se corre con `./gradlew runClientGametest`. Abre una ventana del juego, así que no forma parte de `build` ni de CI. Levanta un server dedicado real, conecta un cliente y verifica:
  - Payloads: la config llega sincronizada, el nivel llega al subirlo con `/tribulation set` y la Blood Moon llega al empezar y al terminar.
  - HUD: el ícono de tier se dibuja en su esquina y desaparece con `enableTierHud=false`.
  - Keybind: mantener Left Alt muestra el panel de detalle centrado.
  - Ítems: el Heart Fragment se ve con su textura en la hotbar y, con Hardcore Hearts activo, el click derecho del cliente devuelve corazones en el server.
- El chequeo de la textura falla si se quitan las definiciones de ítems (352 píxeles violetas en la casilla). Las capturas quedan en `build/clientGametest/screenshots/`.
- La primera vez, el server de test necesita `eula=true` en `build/clientGametest/eula.txt`. La tarea lo escribe sola, igual que el `eula = true` de Loom.
- `./gradlew build`: OK, 910 tests unitarios (907 + 3). `./gradlew runGametest`: OK, 150 gametests.

### Fase 8 (efectos visuales, 2026-11-01)

Los cuatro mixins de cliente volvieron, reescritos contra el render de 26.3 y con la misma matemática (`BloodMoonClientEffects` y `EnvironmentalPressureClientEffects` no cambian de constantes):

| Efecto | 1.21.1 | 26.3 |
|---|---|---|
| Color del cielo | RETURN de `ClientLevel#getSkyColor` | El cielo es el *environment attribute* `SKY_COLOR`: una capa más al final de `ClientLevel#addEnvironmentAttributeLayers`, después del destello del rayo (el mismo lugar del RETURN). La leen el cielo y la niebla atmosférica |
| Niebla | `FogRenderer#setupColor`, antes del último `clearColor` | Final de `renderer/fog/FogRenderer#computeFogColor`, después de los ajustes de vacío, jefe y visión nocturna. Bajo agua o lava sigue vanilla |
| Sol y luna | `@ModifyArgs` sobre el `setShaderColor` compartido por sol y luna | `@ModifyArg` sobre el color que `SkyRenderer#renderSun` y `#renderMoon` pasan a `writeTransform` (`require = 2`: tienen que aplicar los dos). Las estrellas siguen vanilla |
| Oscuridad de Oppressive Nights | RETURN de `LightTexture#calculateDarknessScale` | RETURN del mismo método, ahora en `LightmapRenderStateExtractor`. El shader del lightmap la resta en el mismo orden (restar, recortar, gamma) |

- El client gametest suma un chequeo a medianoche con el HUD oculto: el cielo pasa de (4, 5, 8) a (44, 6, 8) con la Blood Moon, la luna pierde verde y azul, y el suelo se oscurece con Oppressive Nights (el chequeo activa `environmentalPressure.enabled`, que viene apagado). Sin los mixins falla: el cielo queda en (4, 4, 7).
- El rojo del cielo es el de 1.21.1: allá el cielo de medianoche era negro y el tinte lo llevaba a ~(50, 3, 3).
- Con `maxDarkness = 0.25` (valor por defecto) el suelo a medianoche queda casi negro. Es el mismo cálculo que en 1.21.1, no un cambio del port.
- `./gradlew build`: OK, 912 tests unitarios (910 + 2). `./gradlew runGametest`: OK, 150 gametests. `./gradlew runClientGametest`: OK.

### Fase 9 (empaquetado, 2026-11-01)

- El jar de `./gradlew build` trae `fabric.mod.json`, los dos configs de mixins, assets (incluidas las definiciones `items/*.json`) y data. No trae clases de test ni de gametest, ni las integraciones desactivadas (solo `EmiShardRefresh`, que usa reflection).
- Server de producción (no el de desarrollo): Fabric server launcher 26.3 / loader 0.20.1, con `fabric-api-0.165.0+26.3.jar` y el jar del mod en `mods/`. Arranca en ~1 s sin errores ni avisos de mixins y crea `config/tribulation.json`. Por consola: `config`, `reload`, `bloodmoon start`/status/`stop` OK. Un zombie invocado en (3000, 120, 3000) recibe los modificadores `tribulation:distance_*`, `height_*` y `moon_*` en vida, daño, armadura y dureza. Los archivos quedan en `dimensions/minecraft/overworld/data/tribulation/`.
- Cliente real (2026-11-01): Prism en Windows (Minecraft 26.3, loader 0.20.1, `fabric-api-0.165.0+26.3` y el jar del mod) contra ese mismo server en la red local, con cuenta de Microsoft. Pasó toda la checklist §8 salvo multijugador:
  - HUD: la calavera cambia de color por tier, el panel de Left Alt muestra nivel, progreso y las habilidades de los mobs cercanos, y destella en dorado al subir y en celeste al bajar.
  - Comandos `info`, `set`, `debug`, `inspect`, `reload`, `config`, `bloodmoon` y `hearts`.
  - A tier 5 los mobs reciben los modificadores de tiempo, altura y luna, habilidades (sprint, telarañas, flechas de fuego, etc.) y equipamiento. Las partículas de amenaza (disco violeta, `threat_tier.png`) salen en mobs de tier 4+, como en 1.21.1.
  - Shatter Shard (-5 niveles) y Ascendant Shard (+5), las dos recetas, Death Relief (-2) y su espera de 5 minutos.
  - Campeones invocados y naturales (nombre, aura de fuego de almas, `champion_damage` ×1.25, botín extra).
  - Pack Tactics (4 de 5 zombies alertados con un golpe).
  - Blood Moon (cielo, horizonte y spawns duplicados) y su persistencia al reiniciar, junto con el nivel del jugador.
  - Wither por la ruta de jefes: solo vida y daño, +3.0 (tope). La vida queda en 1024, el máximo de vanilla para cualquier entidad.
  - Heart Fragment con Hardcore Hearts activado: devuelve 1 corazón y no se gasta con la vida completa.
- Cosas de 26.3 que no son del mod: el `server.properties` que genera vanilla trae `white-list=true`, y en modo offline la whitelist guarda el UUID de la cuenta real, que no coincide con el UUID offline (se usó `online-mode=true`). El aviso "standing on air - force-sending blocks below" del log es de vanilla. La gamerule ahora es `keep_inventory`.
- Multijugador con 2 jugadores (2026-11-02): el segundo jugador entró desde otra red con Tailscale (Fabric Loader 0.20.1, `fabric-api-0.165.0+26.3` y `gfec18ea`). Con niveles 200 y 20, un husk aparecido cerca de cada uno tomó en `nearest` el nivel del más cercano (24,5 y 64,3 de vida), en `average` el promedio, 110 (44,3 y 44,5), y en `max` el 200 en los dos (el de 32,1 tenía la variante rápida, −10 de vida antes del multiplicador; sus modificadores son los de nivel 200, tier 4). `/tribulation debug` en `max` lista a los dos jugadores y da nivel efectivo 200. El segundo jugador ve su HUD y su panel con su propio nivel. El server quedó otra vez en `nearest`.
- El Dragón no se probó en vivo: usa la misma ruta de jefes que el Wither.

### Integraciones opcionales (T10, 2026-11-01)

Versiones de 26.3 verificadas en Modrinth y en cada Maven el 2026-11-01:

| Integración | Versión | Estado |
|---|---|---|
| Mod Menu | `21.0.0` | Restaurada sin cambios de código |
| Cloth Config | `26.3.159` | Restaurada sin cambios de código |
| Jade | `26.3.5+fabric` | Restaurada; ver abajo |
| REI (+ Architectury) | `26.3.824` (+ `22.0.5`) | Restaurada sin cambios de código |
| JEI | `31.17.0.108` (artefacto `jei-26.3-fabric`) | Restaurada sin cambios de código |
| WTHIT | no hay build de 26.3 (la última, `fabric-20.0.0`, es de 26.2) | Sigue desactivada |
| EMI | no hay build de 26.x (la última, `1.1.24`, es de 1.21.1) | Sigue desactivada |

- Las dependencias son `compileOnly` + `localRuntime` (26.x viene sin ofuscar, no hay remapeo). El POM de la API de REI declara Architectury solo en scope `runtime`, pero las sobrecargas de `EntryStacks.of` nombran su `FluidStack`, así que Architectury también es `compileOnly`.
- Jade: en 26.x `ITooltip` extiende `NarrationSupplier`, una clase de cliente, así que el componente del tooltip ya no compila en el source set `main`. Se separó en dos: `MobScalingJadeProvider` (datos del server, `main`) y `MobScalingJadeComponent` (tooltip, `client`), unidos por el mismo UID. El plugin sigue en `main` (los runs de server en dev no cargan el source set `client`, y Jade tumba el server si un plugin no instancia) y llega al componente por nombre desde `registerClient`, que Jade solo llama en el cliente. Si esa búsqueda falla, se pierde el tooltip y queda un warning en el log; Jade sigue cargando.
- `ManifestEntrypointsCompiledTest` (review R3-001): falla el build si un entrypoint de `fabric.mod.json`, o un literal que nombra una clase del mod en los fuentes (los accessors de `TribulationAPI`, el componente de Jade), apunta a una clase que no se compiló. Se vio fallar con los entrypoints repuestos y los excludes todavía puestos, y con un nombre roto a propósito en el plugin de Jade.
- Pruebas: `./gradlew build` OK (914 tests unitarios); `runGametest` OK (150), con el plugin de Jade cargado en el server; `runClientGametest` OK con Mod Menu, Cloth Config, Jade, REI, Architectury y JEI cargados a la vez, y el plugin de Jade cargado en el cliente sin el warning.
- Prueba en vivo (2026-11-02): Prism en Windows con `tribulation-0.0.0+gfec18ea.jar` contra el server de producción (con Jade instalado) en la red local. Mod Menu: abre la pantalla de Cloth, se cambió «show tier HUD», se guardó, siguió así al reabrir y el HUD cambió en el momento. Jade: un husk escalado muestra «Tribulation Scaled (+263.9% HP)» con 73/73 de vida. REI y JEI (por separado): el panel de información del Shatter Shard muestra los valores del server (nivel 25+, 0,5%, −5 niveles, efectos de 10 s). JEI avisa que el server no tiene JEI; es solo un aviso, el panel es del cliente. El log del cliente no tiene errores. Ojo: el primer intento de Mod Menu falló solo porque el cliente tenía el jar viejo `gf554d89`; revisá la versión en la lista de mods antes de probar.

### Desactivado temporalmente

| Qué | Cómo | Motivo | Cómo restaurarlo |
|---|---|---|---|
| WTHIT | Sin dependencia; `compat/wthit/**` y `waila_plugins.json` excluidos de `main` | No hay build de 26.3 (la última es `fabric-20.0.0`, de 26.2) | Cuando exista: declarar `wthit-api` (`compileOnly`, repo Bai ya está en la base), quitar los dos excludes y verificar la API |
| EMI | Sin dependencia; `compat/emi/EmiShardPlugin.java` excluido de `client` y entrypoint `emi` fuera de `fabric.mod.json`. `EmiShardRefresh` sigue compilando porque usa reflection, no importa EMI y `compat/common/ShardViewerRefresh` lo llama | No hay build de 26.x (la última es `1.1.24`, de 1.21.1) | Cuando exista: repo TerraformersMC, `compileOnly` del artefacto `:api`, quitar el exclude y volver a poner el entrypoint `emi` |

El código de esas dos integraciones sigue en el repo, solo está fuera de la compilación. El bloque `suggests` de `fabric.mod.json` se dejó como estaba: es solo informativo.

### Meridian (resuelto en la Fase 2)

La guía oficial de porting a 26.1 dice: "No existing mods for 1.21.11 or older versions of Minecraft will work on 26.1, even as a compile-only dependency." Meridian solo publicó versiones para 1.21.1, así que salió del build (`modCompileOnly`, el repo ivy de GitHub Releases y `meridian_version`).

Para no cambiar comportamiento, la clase anidada `Api` de `compat/meridian/MeridianEquipmentCompat` ahora llega a `com.rfizzle.meridian.api` por reflection (`MethodHandles`), con el mismo patrón que `EmiShardRefresh`. Si Meridian no está instalado, la clase nunca se carga. Si está pero la API no coincide, el `catch (Throwable)` existente deja el ítem con sus encantamientos vanilla. Los nombres (`getEnchantmentInfo`, `enabled()`, `getMaxLevel()`) se verificaron con `javap` contra `meridian-1.0.0.jar`.

### Versiones de 26.3 (verificadas el 2026-11-01)

Fuentes: rama `26.3` de [FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod/tree/26.3), meta.fabricmc.net y la guía [Porting to 26.1](https://docs.fabricmc.net/develop/porting/).

| Pieza | Antes | Ahora |
|---|---|---|
| Minecraft | 1.21.1 | 26.3 |
| Fabric Loader | 0.16.10 | 0.20.1 (la última estable; la sección 3 decía 0.19.5) |
| Fabric API | 0.116.1+1.21.1 | 0.165.0+26.3 |
| Loom | 1.9-SNAPSHOT, plugin `fabric-loom` | 1.19-SNAPSHOT, plugin `net.fabricmc.fabric-loom` |
| Gradle | 8.12 | 9.8.2 |
| Java | 21 | 25 (también `compatibilityLevel` de los mixins) |
| JaCoCo | 0.8.12 | 0.8.14 (la primera con soporte oficial de Java 25) |

Otros cambios de build: sin línea `mappings` (26.x viene sin ofuscar), `modImplementation` pasa a ser `implementation`, y se quitaron los repos Maven de las integraciones desactivadas (los de REI, Architectury y JEI volvieron en T10).

`versions-common.properties` y `gradle/concord-common.gradle` son archivos de Concord que se editaron a mano en esta rama. Por eso no hay que correr `make sync`.
