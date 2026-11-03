package com.rfizzle.tribulation.config;

import com.rfizzle.tribulation.config.TribulationConfig.MobScaling;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vanilla hostiles added to the per-mob scaling list after the 1.21.1 set:
 * each one resolves to its own default rates and has its own toggle, while the
 * hostiles still left out keep resolving to no scaling.
 */
class ExtendedVanillaScalingTest {

    private static Identifier vanilla(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"parched", "zombie_villager", "piglin_brute", "enderman", "blaze", "breeze", "evoker"})
    void addedHostile_isScaledByDefault(String key) {
        TribulationConfig cfg = new TribulationConfig();

        assertTrue(List.of(TribulationConfig.MOB_KEYS).contains(key), key + " missing from MOB_KEYS");
        assertEquals(Boolean.TRUE, cfg.mobToggles.get(key), key + " toggle should default to on");
        MobScaling resolved = cfg.resolveScalingForEntity(vanilla(key), true);
        assertNotNull(resolved, key + " should resolve to its own scaling");
        assertSame(cfg.scaling.get(key), resolved);
    }

    @ParameterizedTest
    @ValueSource(strings = {"parched", "zombie_villager", "piglin_brute", "enderman", "blaze", "breeze", "evoker"})
    void addedHostile_toggleOff_disablesScaling(String key) {
        TribulationConfig cfg = new TribulationConfig();
        cfg.mobToggles.put(key, false);

        assertNull(cfg.resolveScalingForEntity(vanilla(key), true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"slime", "magma_cube", "vex", "phantom", "ghast", "shulker", "sulfur_cube",
            "creaking", "camel_husk", "zombie_nautilus", "illusioner"})
    void hostileOutsideTheList_staysUnscaled(String key) {
        TribulationConfig cfg = new TribulationConfig();

        assertNull(cfg.resolveScalingForEntity(vanilla(key), true));
    }

    @Test
    void originalHostile_stillResolvesToItsOwnEntry() {
        TribulationConfig cfg = new TribulationConfig();

        assertSame(cfg.scaling.get("zombie"), cfg.resolveScalingForEntity(vanilla("zombie"), true));
    }
}
