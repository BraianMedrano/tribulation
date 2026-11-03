// Tier: 1 (pure JUnit)
package com.rfizzle.tribulation.compat.jade;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jade lists every registered provider in its plugin config screen under
 * {@code config.jade.plugin_<namespace>.<path>} of the provider's UID. Jade 26.x
 * throws in a development client when that key has no translation, and a
 * production client shows the raw key, so the shipped lang file must carry it.
 */
class JadeConfigLangTest {

    private static final Path LANG = Path.of("src/main/resources/assets/tribulation/lang/en_us.json");

    /** Matches {@code MobScalingJadeProvider.UID} ({@code tribulation:mob_scaling}). */
    private static final String MOB_SCALING_CONFIG_KEY = "config.jade.plugin_tribulation.mob_scaling";

    @Test
    void mobScalingProviderHasAConfigTranslation() throws IOException {
        JsonObject lang = new Gson().fromJson(Files.readString(LANG, StandardCharsets.UTF_8), JsonObject.class);

        assertTrue(lang.has(MOB_SCALING_CONFIG_KEY), "en_us.json is missing " + MOB_SCALING_CONFIG_KEY);
    }
}
