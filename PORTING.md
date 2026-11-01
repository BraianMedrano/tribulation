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
- [ ] Mod Menu y Cloth Config de 26.3 *solo si existen versiones para 26.3* **[no verificado]**; el mod funciona sin ellos.

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
- [ ] `/tribulation info`: ver nivel, tier y progreso.
- [ ] `/tribulation set <jugador> 250`: llevar a tier 5 para ver todas las habilidades.
- [ ] `/tribulation inspect` y `/tribulation debug <jugador>`: ver el desglose de escalado de un mob.
- [ ] `/tribulation reload`: recargar `config/tribulation.json`.
- [ ] `/tribulation bloodmoon start` y `stop`.

Comportamiento:
- [ ] Spawnear zombie, esqueleto, araña y creeper: verificar vida/daño y habilidades por tier.
- [ ] Equipamiento de mobs por tier.
- [ ] Muerte → Death Relief (pierde 2 niveles).
- [ ] Craftear y usar Shatter Shard y Ascendant Shard.
- [ ] Heart Fragment (si activás Hardcore Hearts).
- [ ] Élites (nivel 50+): afijos y drops.
- [ ] Pack Tactics (tier 3+).
- [ ] Jefes (Dragon/Wither): escalado más suave.
- [ ] Reinicio del server: el nivel debe persistir.
- [ ] Con 2 jugadores: modos de multijugador (nearest / average / max).

Cliente:
- [ ] HUD visible y correcto; panel de detalle con Left Alt.
- [ ] Config sincronizada desde el server al conectarse.

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
- Si existen versiones 26.3 de Mod Menu, Cloth Config, Jade y EMI/REI.
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
- Pendiente de verificar en fases siguientes: los `SavedData` ahora se guardan en `dimensions/minecraft/overworld/data/tribulation/` (ruta de 26.x), así que un mundo actualizado desde 1.21.1 arrancaría sin el estado de Tribulation (Fase 6); faltan las definiciones `assets/tribulation/items/*.json` que pide 26.x para el modelo de cada ítem (Fase 7); hay que regenerar datagen por el cambio de id del fondo de advancements (Fase 4).

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

### Desactivado temporalmente

| Qué | Cómo | Motivo | Cómo restaurarlo |
|---|---|---|---|
| Mod Menu + Cloth Config (pantalla de config) | Dependencias quitadas de `build.gradle`, `compat/modmenu/**` excluido del source set `client`, entrypoint quitado de `fabric.mod.json` | Las versiones fijadas eran de 1.21.1 (`modmenu_version=11.0.4`, `cloth_config_version=15.0.140`). Mientras tanto la config se edita en `config/tribulation.json` y se aplica con `/tribulation reload` | Conseguir versiones 26.3, volver a declarar las dependencias y el entrypoint, y quitar el exclude |
| Jade | Igual, `compat/jade/**` excluido del source set `main` | `jade_version=15.10.5+fabric` era de 1.21.1 | Igual |
| WTHIT | Igual, `compat/wthit/**` y `waila_plugins.json` excluidos de `main` | `wthit_version=fabric-12.10.2` era de 1.21.1 | Igual, y volver a incluir `waila_plugins.json` |
| EMI | Igual, `compat/emi/EmiShardPlugin.java` excluido de `client`. `EmiShardRefresh` sigue compilando porque usa reflection, no importa EMI y `compat/common/ShardViewerRefresh` lo llama | `emi_version=1.1.22+1.21.1` | Igual |
| REI (+ Architectury en dev) | Igual, `compat/rei/**` excluido de `client` | `rei_version=16.0.799`, `architectury_version=13.0.6` | Igual |
| JEI | Igual, `compat/jei/**` excluido de `client` | El artefacto se llama `jei-1.21.1-fabric`, `jei_version=19.27.0.340` | Igual |
| Efectos visuales de Blood Moon (niebla, color de cielo, tinte de luna) y oscuridad de "Oppressive Nights" | `mixin/client/**` excluido del source set `client` y la lista `client` de `tribulation.client.mixins.json` vacía | 26.x reescribió ese render: `FogRenderer.setupColor` y `RenderSystem.clearColor` ya no existen (la niebla pasó a `renderer/fog/FogRenderer` y a *environment attributes*), y `LightTexture` ya no existe: `calculateDarknessScale` ahora es privado en `LightmapRenderStateExtractor`. Con `defaultRequire: 1`, un mixin que no aplica crashea el cliente al arrancar. La lógica pura (`BloodMoonClientEffects`, `EnvironmentalPressureClientEffects`) sigue compilando; el servidor sigue mandando los payloads | Fase 8: reescribir los cuatro mixins contra el render de 26.3, volver a listarlos en el JSON y quitar el exclude |

El código de esas integraciones sigue en el repo, solo está fuera de la compilación. El bloque `suggests` de `fabric.mod.json` se dejó como estaba: es solo informativo, y `ClothGuardTest` (guard de Concord que lee los fuentes del disco) exige `cloth-config` mientras `TribulationConfigScreen` exista.

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

Otros cambios de build: sin línea `mappings` (26.x viene sin ofuscar), `modImplementation` pasa a ser `implementation`, y se quitaron los repos Maven de las integraciones desactivadas.

`versions-common.properties` y `gradle/concord-common.gradle` son archivos de Concord que se editaron a mano en esta rama. Por eso no hay que correr `make sync`.
