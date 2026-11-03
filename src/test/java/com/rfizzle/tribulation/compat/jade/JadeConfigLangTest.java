// Tier: 1 (pure JUnit)
package com.rfizzle.tribulation.compat.jade;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rfizzle.tribulation.Tribulation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Jade lists every registered provider in its plugin config screen under
 * {@code config.jade.plugin_<namespace>.<path>} of the provider's UID. Jade 26.x
 * throws in a development client when that key has no translation, and a
 * production client shows the raw key, so the shipped lang file must carry it.
 */
class JadeConfigLangTest {

    private static final String LANG = "assets/tribulation/lang/en_us.json";

    /** The key Jade's plugin config screen looks up for a provider UID path. */
    private static String configKey(String uidPath) {
        return "config.jade.plugin_" + Tribulation.MOD_ID + "." + uidPath;
    }

    @Test
    void mobScalingProviderHasAConfigTranslation() throws IOException {
        String key = configKey(JadeProviderIds.MOB_SCALING);
        JsonElement value = shippedLang().get(key);

        assertNotNull(value, LANG + " is missing " + key);
        assertFalse(value.getAsString().isBlank(), LANG + " has a blank translation for " + key);
    }

    private JsonObject shippedLang() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(LANG)) {
            assertNotNull(in, LANG + " is not on the classpath");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader, JsonObject.class);
            }
        }
    }
}
