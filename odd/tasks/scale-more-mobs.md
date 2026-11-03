# Scale more vanilla hostiles

Objective: extend Tribulation's per-mob scaling to vanilla hostiles it skips today, starting with group 1; stats first, abilities later.
Branch: `feat/scale-more-mobs` (from `mc/26.3` @ 659f50e). Delivery: `ask-on-risk`. Runners: `./gradlew test`, `./gradlew runGametest`.

## Specs

- S1 Group 1, user-approved ("Si" to "¿Arranco una rama nueva desde `mc/26.3` con el grupo 1?"): Parched, Zombie Villager, Piglin Brute, Enderman, Blaze, Breeze, Evoker get Tribulation's per-mob scaling (health, damage, armor and the other stat axes) by default, each with its own default rates and its own `mobToggles` switch.
- S2 Done in code, not only in config, so it ships as part of the fork (agreed: "en el código, no en la configuración, para que quede como parte de tu versión del mod").
- S3 Stats first; mob-specific abilities are a later phase (agreed: "Arrancaría solo con estadísticas ... las habilidades propias de cada uno vendrían en una fase posterior").
- S4 Separate branch from the port (user: "de todas maneras lo hagamos en otra rama").
- S5 Existing configs pick up the new mobs without manual edits (assumption: the existing `fillDefaults()` merge of missing `scaling` keys covers it; verify).
- S6 No armor/weapon equipment or zombie/skeleton variants on mobs where it makes no sense (assumption from review of `MobScalingHandler.applyScaling`; verify which handlers gate by type).
- S7 Verify the Zombie Villager cure case: a cured villager must not keep Tribulation's extra stats (open question raised in conversation).
- S8 Groups 2 (Slime, Magma Cube, Vex, Phantom, Ghast, Shulker, Sulfur Cube) and the excluded mobs (Creaking, Camel Husk, Zombie Nautilus, Warden, Elder Guardian) are out of scope.

## Tasks

| ID | Specs | Route | Status | Evidence |
|---|---|---|---|---|
| T1 | S5, S6 | delegated (explorer: mapping beyond inline budget) | done | Map of gating and touchpoints in L3 |
| T2 | S1, S2, S3, S5 | inline | done | RED: ExtendedVanillaScalingTest 7 failed; gametest `added_hostiles_get_time_axis_health` failed ("parched missing tribulation_processed"). GREEN: 969 unit, 156 gametests |
| T3 | S6 | inline | done | Evoker excluded from weapons. RED: `weapon_skipped_for_evoker` ("had minecraft:iron_sword"); pillager regression test passes before and after |
| T4 | S7 | inline | done | Stats already vanilla after cure (conversion copies no modifiers). `ConversionCleanupHandler` strips `tribulation_*` tags and a champion's name on hostile-to-non-hostile conversion. RED: "cured villager kept tag tribulation_processed"; player name tag and zombie-to-drowned tests pass before and after |
| T5 | S1 | inline | partial | `./gradlew build` OK (969 unit), `runGametest` 156/156; live check pending |

## Log

- L1 (2026-11-03) User: "no deberiamos primero hacer que tribulation aplique a los mobs que no aplica primero?" / "enserio es cambio de meacnica? si solo seria extender el mod a los mobs que salieron no? de todas maneras lo hagamos en otra rama" / "que opinas vos? agregamos los nuevos mobs y escalamos los viejos? no creo que el creador nos conteste" / "Si" (to starting group 1 on a new branch).
- L2 (2026-11-03) Port branch `mc/26.3` pushed to origin (BraianMedrano/tribulation) at 659f50e before branching. Live evidence from 2026-11-02: the 7 group-1 mobs get no `tribulation:*` modifiers today; `resolveScalingForEntity` only scales `minecraft:` mobs listed in `scaling`; the modded fallback excludes the `minecraft` namespace. `MobScalingHandler.applyScaling` runs armor, abilities, weapons and zombie/skeleton variants for any mob with a toggle key, so adding keys may hand armor/weapons to Blaze/Enderman/Breeze unless those handlers gate by type (T1).
- L3 (2026-11-03) T1 map: Parched extends AbstractSkeleton, ZombieVillager extends Zombie, PiglinBrute extends AbstractPiglin, Evoker extends AbstractIllager; armor goes to Zombie/AbstractSkeleton/AbstractPiglin, weapons also to AbstractIllager (so Evoker would get a sword); zombie/skeleton variants use fixed key lists (new keys excluded); abilities key on exact mob key (none for new keys); champions, night senses, group health, xp apply to any scaled Enemy. `fillDefaults()` merges missing `scaling`/`mobToggles` keys in memory; the file is rewritten only after a migration, so no version bump is needed. Conversion (`Mob.convertTo`) copies tags, custom name and custom data but no attribute modifiers or attachments.
- L4 (2026-11-03) T2-T4 implemented. Default rates follow the closest existing family (parched = skeleton, zombie_villager = zombie, piglin_brute near hoglin, evoker = witch, enderman/blaze/breeze with slow speed scaling); all satisfy the existing cap = rate x 250 invariants. Docs: mobs page cards, features and SPEC rate tables, config toggle example, counts 21 -> 28 in site, listings, README, SPEC. Site pages still say Minecraft 1.21.1 (pre-existing on the port branch; out of scope). Next: live check on the test server.
- L5 (2026-11-03) Independent verifier on d2ae8cd: no blockers, S1-S8 met; `./gradlew test --rerun build` 969 unit, `runGametest` 156/156. RDD whole branch from 6d4fdc3 hit `lens_context_budget_exceeded` again; reduced to 659f50e..d2ae8cd (reliability, lineage review-9d5298c42b98c7fd): user granted, approved and acknowledged. Advisories from both: a renamed champion zombie villager lost the player's name on cure; the name-tag gametest could flake on a champion roll; stale "21" comment in `MobScalingHandler`. Fixed: the cleanup now clears the name only when it still equals the generated champion name (RED: `cured_champion_zombie_villager_keeps_player_rename` "had null"), cure tests spawn with the champion roll off, comment updated. 969 unit, 157/157 gametests.

