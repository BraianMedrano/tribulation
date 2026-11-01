package com.rfizzle.tribulation.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every registered item ships the full 26.x model chain: the item definition under
 * {@code assets/tribulation/items/} (required since 1.21.4, without it the item renders as the
 * missing-model checker), the flat model it points at, and the texture that model's
 * {@code layer0} names.
 */
class ItemModelResourceContractTest {
    // Compile-time constants: reading them does not initialize TribulationItems.
    private static final List<String> ITEM_PATHS = List.of(
            TribulationItems.SHATTER_SHARD_PATH,
            TribulationItems.ASCENDANT_SHARD_PATH,
            TribulationItems.HEART_FRAGMENT_PATH);

    @TestFactory
    Stream<DynamicTest> everyItemHasADefinitionModelAndTexture() {
        return ITEM_PATHS.stream().map(path -> DynamicTest.dynamicTest(path, () -> {
            JsonObject definition = json("/assets/tribulation/items/" + path + ".json");
            JsonObject model = definition.getAsJsonObject("model");
            assertNotNull(model, path + ": item definition has no \"model\" object");
            assertEquals("minecraft:model", model.get("type").getAsString(), path + ": model type");
            assertEquals("tribulation:item/" + path, model.get("model").getAsString(), path + ": model id");

            JsonObject itemModel = json("/assets/tribulation/models/item/" + path + ".json");
            String parent = itemModel.get("parent").getAsString();
            assertEquals("minecraft:item/generated", parent.contains(":") ? parent : "minecraft:" + parent,
                    path + ": flat items parent item/generated");
            String layer0 = itemModel.getAsJsonObject("textures").get("layer0").getAsString();
            assertEquals("tribulation:item/" + path, layer0, path + ": layer0 texture");
            assertNotNull(ItemModelResourceContractTest.class.getResource(
                    "/assets/tribulation/textures/item/" + path + ".png"), path + ": texture png is missing");
        }));
    }

    private static JsonObject json(String resource) throws IOException {
        try (InputStream in = ItemModelResourceContractTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing resource " + resource);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
