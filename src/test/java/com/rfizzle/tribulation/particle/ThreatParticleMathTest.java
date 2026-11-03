// Tier: 1 (pure JUnit)
package com.rfizzle.tribulation.particle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatParticleMathTest {

    private static final double EPS = 1e-6;

    // --- fade (shared by every threat cue; behaviour kept from before) ---

    @Test
    void fadeAlpha_easesOutQuadratically() {
        assertEquals(0.8f, ThreatParticleMath.fadeAlpha(0.8f, 0, 40), EPS);
        assertEquals(0.6f, ThreatParticleMath.fadeAlpha(0.8f, 20, 40), EPS);
        assertEquals(0.0f, ThreatParticleMath.fadeAlpha(0.8f, 40, 40), EPS);
    }

    @Test
    void fadeAlpha_zeroLifetime_isFullyFaded() {
        assertEquals(0.0f, ThreatParticleMath.fadeAlpha(0.8f, 0, 0), EPS);
    }

    // --- smoke growth (S1: 0.20 to 0.48 block across the life) ---

    @Test
    void grownSize_swellsFromBaseToBasePlusGrow() {
        assertEquals(0.10f, ThreatParticleMath.grownSize(0.10f, 0.24f, 0, 40, 0f), EPS);
        assertEquals(0.22f, ThreatParticleMath.grownSize(0.10f, 0.24f, 20, 40, 0f), EPS);
        assertEquals(0.34f, ThreatParticleMath.grownSize(0.10f, 0.24f, 40, 40, 0f), EPS);
    }

    @Test
    void grownSize_interpolatesWithPartialTick() {
        assertEquals(0.106f, ThreatParticleMath.grownSize(0.10f, 0.24f, 0, 40, 1f), EPS);
    }

    @Test
    void grownSize_noGrowth_keepsBaseSize() {
        assertEquals(0.16f, ThreatParticleMath.grownSize(0.16f, 0f, 30, 40, 0.5f), EPS);
    }

    @Test
    void grownSize_neverExceedsFullGrowth() {
        assertEquals(0.34f, ThreatParticleMath.grownSize(0.10f, 0.24f, 45, 40, 0.5f), EPS);
    }

    // --- light floor (S2) ---

    @Test
    void flooredBlockLight_raisesDarkLightToTheFloor() {
        assertEquals(9, ThreatParticleMath.flooredBlockLight(0, ThreatParticleMath.SMOKE_BLOCK_LIGHT_FLOOR));
        assertEquals(9, ThreatParticleMath.flooredBlockLight(4, 9));
    }

    @Test
    void flooredBlockLight_keepsBrighterLight() {
        assertEquals(14, ThreatParticleMath.flooredBlockLight(14, 9));
    }

    @Test
    void smokeFloor_isBlockLightNine() {
        assertEquals(9, ThreatParticleMath.SMOKE_BLOCK_LIGHT_FLOOR);
    }

    // --- champion orbit (S3) ---

    @Test
    void orbitRadius_zombieWidth_matchesPreview() {
        assertEquals(0.55, ThreatParticleMath.orbitRadius(0.6), EPS);
    }

    @Test
    void orbitRadius_widerMob_orbitsOutsideItsBody() {
        // A ravager (1.95 wide) must not have sparks inside its hitbox.
        double r = ThreatParticleMath.orbitRadius(1.95);
        assertTrue(r > 1.95 / 2, "radius " + r + " should clear the half-width");
    }

    @Test
    void orbitRadius_tinyMob_keepsMinimumRadius() {
        assertEquals(0.55, ThreatParticleMath.orbitRadius(0.3), EPS);
    }

    @Test
    void climbHeight_risesFromFeetToHead() {
        assertEquals(0.15, ThreatParticleMath.climbHeight(1.95, 0, 44), EPS);
        assertEquals(1.95 * 0.95, ThreatParticleMath.climbHeight(1.95, 44, 44), EPS);
        double mid = ThreatParticleMath.climbHeight(1.95, 22, 44);
        assertEquals((0.15 + 1.95 * 0.95) / 2, mid, EPS);
    }

    @Test
    void orbitAlpha_fadesInThenOut() {
        assertEquals(0.0f, ThreatParticleMath.orbitAlpha(0, 44), EPS);
        assertEquals(0.5f, ThreatParticleMath.orbitAlpha(2, 44), EPS);
        assertEquals(1.0f, ThreatParticleMath.orbitAlpha(20, 44), EPS);
        assertEquals(0.5f, ThreatParticleMath.orbitAlpha(41, 44), EPS);
        assertEquals(0.0f, ThreatParticleMath.orbitAlpha(44, 44), EPS);
    }

    @Test
    void orbitAlpha_staysWithinUnitRange() {
        for (int age = -2; age <= 50; age++) {
            float a = ThreatParticleMath.orbitAlpha(age, 44);
            assertTrue(a >= 0f && a <= 1f, "alpha " + a + " at age " + age);
        }
    }
}
