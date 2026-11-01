// Tier: 1 (pure JUnit)
package com.rfizzle.tribulation.client;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloodMoonClientEffectsTest {

    private static final float EPSILON = 1.0e-6f;

    @Test
    void tintSky_pullsEachChannelSixtyFivePercentTowardBloodRed() {
        Vector3fc tinted = BloodMoonClientEffects.tintSky(new Vector3f(0.0f, 0.0f, 0.0f));

        assertEquals(0.30f * 0.65f, tinted.x(), EPSILON);
        assertEquals(0.02f * 0.65f, tinted.y(), EPSILON);
        assertEquals(0.02f * 0.65f, tinted.z(), EPSILON);
    }

    @Test
    void tintSky_leavesTheSourceColorUnchanged() {
        Vector3f sky = new Vector3f(0.47f, 0.65f, 1.0f);

        BloodMoonClientEffects.tintSky(sky);

        assertEquals(new Vector3f(0.47f, 0.65f, 1.0f), sky);
    }
}
