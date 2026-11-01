# Port Tribulation to Minecraft 26.3

Objective: port the fork from 1.21.1 to 26.3 (Java 25, Fabric Loader 0.19.5+, Fabric API `0.161.0+26.3`) without changing gameplay.
Branch: `mc/26.3` (from `master` @ 3ac1524). Delivery: `ask-on-risk`. Runners: `./gradlew test`, `./gradlew runGametest`. Phase plan and port status: `PORTING.md`.

## Specs

- S1 "El fork compila en 26.3 y `./gradlew test` pasa."
- S2 "`./gradlew runGametest` pasa."
- S3 "El server dedicado arranca y el escalado de mobs funciona (comandos `/tribulation ...`)."
- S4 "Un cliente se conecta, ve el HUD y los ítems funcionan."
- S5 "Efectos visuales (Blood Moon, oscuridad de noche) funcionan. **Este paso es opcional**: si se complica, se desactiva y el mod sigue siendo usable."
- S6 Rules, verbatim from `PORTING.md` §9: "No cambiar mecánicas de juego ni balance: este es un port, no una versión nueva." / "Un commit por fase, con mensaje claro." / "Si algo se desactiva temporalmente (HUD, Blood Moon visual), anotarlo en `PORTING.md` con el motivo." / "Nunca borrar o relajar tests para que pasen." / "Después de cada fase: `./gradlew build` y `./gradlew test`." / "Ante dudas de API en 26.x, consultar la documentación oficial de Fabric y el código fuente de Minecraft de esa versión; **no inventar nombres de clases o métodos**." / "Si un mixin no aplica, explicar por qué (qué cambió en la clase objetivo)."
- S7 Do not run `make sync` on the port branch (assumption agreed in conversation: it would overwrite the 26.3 build files).

## Tasks

| ID | Phase | Specs | Route | Status | Evidence |
|---|---|---|---|---|---|
| T0 | 0 Baseline on 1.21.1 | S1, S2 | inline | done | build OK, 896 unit tests, 147 gametests (JDK 21) |
| T1 | 1 Disable optional integrations | S6 | inline | done | build OK, 896 unit tests, 147 gametests on 1.21.1; disabled classes absent from the jar; commit 7259df0 |
| T2 | 2 Build files to 26.3 (+ Meridian decision, minimal Concord decoupling) | S1, S6, S7 | inline | done | Gradle 9.8.2 + Loom 1.19.3 configure, MC 26.3 resolves, `compileJava` reaches javac (API errors expected for T3) |
| T3 | 3 Compile fixes, common logic first | S1, S6 | inline | in progress | mechanical pass: `main` errors 408 -> 229 |
| T4 | 4 Unit tests green | S1, S6 | inline | pending | |
| T5 | 5 Gametests green | S2, S6 | inline | pending | |
| T6 | 6 Dedicated test server + command checklist | S3 | inline | pending | |
| T7 | 7 Client: HUD, payloads, peek keybind | S4 | inline | pending | |
| T8 | 8 Visual effects (optional) | S5 | inline | pending | |
| T9 | 9 Package and test in the real client | S3, S4 | inline | pending | |
| T10 | Restore optional integrations with 26.3 builds; add a test that every class named in `fabric.mod.json` entrypoints is compiled (review R3-001) | S6 | inline | pending | |

## Log

- L1 (2026-11-01) User's original request: "Te adjunto este archivo como contexto de lo que quiero hacer, quiza lo haga con otra persona mas, que opinas  /Users/braian/Downloads/tribulation-26.3-port.md, quiza tambien lo vaya actualizando para versiones nuevas, por lo que quza tamien se suba a modrinth y demas, que opinas? cambiamos el nombre del repo en github? el de la carpeta tambien?" The attached plan is committed as `PORTING.md`.
- L2 (2026-11-01) Opened upstream issue rfizzle/tribulation#265 asking about a 26.x port. Repo renamed to BraianMedrano/tribulation; name and mod id change deferred until rfizzle answers.
- L3 (2026-11-01) T0 done on JDK 21 (Gradle 8.12 does not run on Java 25). Two pre-existing mixin mapping warnings in the Blood Moon client mixins.
- L4 (2026-11-01) T1: Meridian kept in the build because main code calls `compat/meridian` directly; decision moved to T2.
- L5 (2026-11-01) T1: `compat/common/ShardViewerRefresh` calls `compat/emi/EmiShardRefresh` (reflection only, no EMI imports), so only `EmiShardPlugin` is excluded from EMI. `ClothGuardTest` (hub-owned guard) reads sources from disk and requires `cloth-config` in `suggests` while `TribulationConfigScreen` exists, so the `suggests` block was kept unchanged.
- L6 (2026-11-01) T1 review (lineage review-da6de54ee464b058, reliability lens): approved and acknowledged. Advisory R3-001 (entrypoints and source excludes are kept in sync by hand; a mismatch crashes only when the optional mod is installed) deferred to T10 by user decision.
- L7 (2026-11-01) T2: Meridian dropped from the build (official 26.1 porting guide: "No existing mods for 1.21.11 or older versions of Minecraft will work on 26.1, even as a compile-only dependency."); its nested `Api` now uses reflection, names verified with javap against meridian-1.0.0.jar. Build is red by design until T3: first `compileJava` hits javac's 100-error cap (top symbols: `ResourceLocation` x52, `net.minecraft.advancements.critereon` package, `InteractionResultHolder`, `TrialSpawnerData`, `AbstractArrow`).
- L8 (2026-11-01) T3 mechanical pass: `ResourceLocation` -> `Identifier` (the four statics used exist on `Identifier`), and imports rewritten only where exactly one class with that simple name exists in the 26.3 jar (e.g. `monster.Zombie` -> `monster.zombie.Zombie`, `advancements.critereon.*` -> `advancements.triggers`/`advancements.predicates.entity`, `GameRules` -> `gamerules.GameRules`). `main` errors 408 -> 229 in 48 files. Remaining are real API changes: `getTags()` x26, `InteractionResultHolder` x26, `getName()` x15, `hasPermission(int)` x10, `getServer()` x8, `ResourceKey.location()` x7, `displayClientMessage` x7, `TrialSpawnerData` (now `TrialSpawnerStateData`), `playS2C()`, `isDay()`, `getMoonPhase()`, `SavedData.Factory`, `GameRules.RULE_MOBGRIEFING`, `SwordItem`/`ArmorItem`/`DiggerItem`, `MobSpawnType`, `ContextAwarePredicate`, `DimensionDataStorage`. Client, gametest and test source sets not compiled yet. Tool: compile with `--init-script` that adds `-Xmaxerrs 10000` (kept outside the repo).
- L9 (2026-11-01) T2 Risk: item 5 (dependencies and toolchain). RDD assess on 6d4fdc3..651e423: medium, review_due false (under_budget). Independent verification deferred until the build is green.
