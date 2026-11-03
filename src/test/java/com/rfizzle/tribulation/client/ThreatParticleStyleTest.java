// Tier: 1 (pure JUnit)
package com.rfizzle.tribulation.client;

import com.rfizzle.tribulation.particle.ThreatParticleMath;
import net.minecraft.util.LightCoordsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreatParticleStyleTest {

    @Test
    void tierSmoke_raisesDarkBlockLightToTheFloorAndKeepsSkyLight() {
        int dark = LightCoordsUtil.pack(0, 7);
        assertEquals(LightCoordsUtil.pack(ThreatParticleMath.SMOKE_BLOCK_LIGHT_FLOOR, 7),
                ThreatParticle.Style.TIER.lightCoords(dark));
    }

    @Test
    void tierSmoke_keepsBrighterBlockLight() {
        int bright = LightCoordsUtil.pack(13, 2);
        assertEquals(bright, ThreatParticle.Style.TIER.lightCoords(bright));
    }

    @Test
    void bigAndSpeedCues_takeTheWorldLightAsIs() {
        int dark = LightCoordsUtil.pack(2, 5);
        assertEquals(dark, ThreatParticle.Style.BIG.lightCoords(dark));
        assertEquals(dark, ThreatParticle.Style.SPEED.lightCoords(dark));
    }
}
