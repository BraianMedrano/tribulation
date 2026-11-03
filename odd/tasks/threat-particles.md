# Threat particles: dark red smoke + golden champion orbit

Objective: replace the tier-4+ threat cue with a dark red smoke puff and the champion soul-fire aura with golden orbiting sparks.
Branch: `feat/threat-particles` (from `develop`). Delivery: single PR into `develop`. Test runner: `./gradlew test`, `./gradlew runGametest`.

## Specs

- **S1** Tier cue = "Dark red smoke" exactly as previewed (https://claude.ai/artifact/FaXJrUmVVFQFeM7iBiukoe): a puff that swells, darkens and thins out as it rises; 6 frames chosen by age; size 0.20 to 0.68 block wide (half-size 0.10 + 0.24 growth, as the preview animation draws it); lifetime 1.4 to 2.2 s; slow rise. User: "El dark red me gusta tal cual lo propones vos".
- **S2** Smoke lighting = brightness floor: it takes the world light but never drops below about block light 9 (recommended; user answered "Si" to implementing "con el humo usando el piso de brillo").
- **S3** Champion aura = "Golden orbit" exactly as previewed: gold sparks that circle the body and climb slowly from feet to head; replaces the vanilla blue soul fire. User: "el golden orbit me encanta".
- **S4** Big and Speed zombie cues keep their current look and motion (shared particle class).
- **S5** Docs follow the change: site feature/champion copy, config tooltip, Javadoc.

## Tasks

| ID | Specs | Route | Status | Evidence |
|----|-------|-------|--------|----------|
| T1 | S1, S3 | inline | done | `glyph.py --verify-all`: 10 verified, 0 drifted |
| T2 | S1, S2, S4 | inline | done | `ThreatParticleMathTest` RED (missing class) then GREEN 15/15 |
| T3 | S3 | inline | done | `ParticleRegistrationGameTest` asserts `champion_aura` |
| T4 | S5 | inline | done | site features/config copy, minimum-tier tooltip |
| T5 | S1-S4 | inline | partial | build green (985 unit tests), 157/157 gametests; live: golden orbit confirmed (L7), client log has no missing particle sprites; user confirmation of the smoke pending |

## Log

- L1 (2026-11-03) User: "Si, eso iria luego de velop para tener todas las modificaciones de nuestros cambios y posible mod propio despegado de tribulation?" — in reply to "¿Lo implemento en una rama nueva `feat/threat-particles`, con el humo usando el piso de brillo?"
- L2 Branch cut from `develop` (88eb1a8). The future separate mod lives in its own repo; this work belongs to the user's Tribulation (`develop`).
- L3 Smoke palette moved onto tokens (`tribulation.ember-1..-4`, `smoke-3..-4`); slightly more saturated than the preview's raw hex, same shapes (generator mirrors the preview hash bit for bit). S4 kept by keeping BIG/SPEED style values, friction 0.94, random sprite and world light.
- L5 Native review of dcbb41f (readability + reliability) approved and acknowledged. Advisory R2-smoke-size-doc-mismatch: the preview card label said "0.20 to 0.48 block" while its animation (and the code) grow 0.20 to 0.68 block wide. Kept the size the user saw; corrected S1, the test comment and the preview label.
- L6 Stop hook offered the whole branch from 6d4fdc3 (186 files, 5652 lines); user granted; START refused `lens_context_budget_exceeded`, nothing created. Reduced scope: 09f6b7b assessed against the reviewed boundary dcbb41f: medium, `under_budget`, stays pending in the slice.
- L7 (2026-11-03) Live check on the Mac (`runClient`, level 250, champion chance 1.0 in the dev run config): golden sparks orbit the champion skeletons in a dark cave (user screenshot). Out of scope, found during the check: every tier 4+ skeleton burns without damage and shoots unlit arrows (pre-existing `skeleton_flame_arrows`, MobAbilities.java:68; MC 26.3 never lights an arrow from a burning shooter). User: "dale, arreglalo así con flechas reales y sin fuego, pero seria en otra sesion no?" — separate fix, next session.
- L4 The orbit spark receives the champion's entity id and start angle through the spawn velocity arguments; the provider returns null when the entity is gone.
