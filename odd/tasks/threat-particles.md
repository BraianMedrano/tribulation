# Threat particles: dark red smoke + golden champion orbit

Objective: replace the tier-4+ threat cue with a dark red smoke puff and the champion soul-fire aura with golden orbiting sparks.
Branch: `feat/threat-particles` (from `develop`). Delivery: single PR into `develop`. Test runner: `./gradlew test`, `./gradlew runGametest`.

## Specs

- **S1** Tier cue = "Dark red smoke" exactly as previewed (https://claude.ai/artifact/FaXJrUmVVFQFeM7iBiukoe): a puff that swells, darkens and thins out as it rises; 6 frames chosen by age; size 0.20 to 0.48 block; lifetime 1.4 to 2.2 s; slow rise. User: "El dark red me gusta tal cual lo propones vos".
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
| T5 | S1-S4 | inline | pending | build green (985 unit tests), 157/157 gametests; live `runClient` check pending |

## Log

- L1 (2026-11-03) User: "Si, eso iria luego de velop para tener todas las modificaciones de nuestros cambios y posible mod propio despegado de tribulation?" — in reply to "¿Lo implemento en una rama nueva `feat/threat-particles`, con el humo usando el piso de brillo?"
- L2 Branch cut from `develop` (88eb1a8). The future separate mod lives in its own repo; this work belongs to the user's Tribulation (`develop`).
- L3 Smoke palette moved onto tokens (`tribulation.ember-1..-4`, `smoke-3..-4`); slightly more saturated than the preview's raw hex, same shapes (generator mirrors the preview hash bit for bit). S4 kept by keeping BIG/SPEED style values, friction 0.94, random sprite and world light.
- L4 The orbit spark receives the champion's entity id and start angle through the spawn velocity arguments; the provider returns null when the entity is gone.
