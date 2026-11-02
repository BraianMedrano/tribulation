package com.rfizzle.tribulation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard that every class the mod names by string, in the shipped manifest's entrypoints or in a
 * reflective lookup in its sources, is actually compiled.
 *
 * <p>The optional integrations are wired in two places kept in sync by hand: the entrypoint in
 * {@code fabric.mod.json} and the source set that compiles the adapter class. If a class is
 * excluded from compilation (or renamed, or deleted) while its entrypoint stays, nothing fails
 * until a player installs that optional mod, and then Fabric Loader crashes at startup with a
 * {@code ClassNotFoundException}. This test fails the build instead.
 *
 * <p>Classes are looked up as resources rather than loaded: an adapter references the optional
 * mod's API, which is compile-only and absent from the test runtime, so loading it would fail
 * for the wrong reason. The test classpath carries both the {@code main} and {@code client}
 * outputs, which covers every entrypoint environment.
 *
 * <p>The same failure hides behind the reflective bridges from the common source set into the
 * client one (the {@code TribulationAPI} client accessors, the Jade tooltip component): a stale
 * name there degrades silently at runtime, so every string literal naming a mod class is checked.
 */
class ManifestEntrypointsCompiledTest {
    private static final Path SHIPPED_MANIFEST = Path.of("src/main/resources/fabric.mod.json");
    private static final List<Path> SOURCE_ROOTS = List.of(Path.of("src/main/java"), Path.of("src/client/java"));
    private static final Pattern MOD_CLASS_LITERAL = Pattern.compile("\"(com\\.rfizzle\\.tribulation\\.[\\w.$]+)\"");

    @Test
    void everyEntrypointClassIsCompiled() throws IOException {
        JsonObject manifest = JsonParser.parseString(Files.readString(SHIPPED_MANIFEST, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject entrypoints = manifest.getAsJsonObject("entrypoints");

        List<String> checked = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : entrypoints.entrySet()) {
            for (JsonElement declared : entry.getValue().getAsJsonArray()) {
                String className = entrypointClass(declared);
                checked.add(className);
                String resource = className.replace('.', '/') + ".class";
                if (getClass().getClassLoader().getResource(resource) == null) {
                    missing.add(entry.getKey() + " -> " + className);
                }
            }
        }

        assertFalse(checked.isEmpty(), "no entrypoints found in " + SHIPPED_MANIFEST);
        assertTrue(missing.isEmpty(), "entrypoints in " + SHIPPED_MANIFEST
                + " name classes that are not compiled (excluded from a source set, renamed, or deleted): " + missing);
    }

    @Test
    void everyClassNamedByStringInSourcesIsCompiled() throws IOException {
        List<String> checked = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Path root : SOURCE_ROOTS) {
            List<Path> files;
            try (Stream<Path> tree = Files.walk(root)) {
                files = tree.filter(p -> p.toString().endsWith(".java")).toList();
            }
            for (Path file : files) {
                Matcher literal = MOD_CLASS_LITERAL.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (literal.find()) {
                    String className = literal.group(1);
                    checked.add(className);
                    if (getClass().getClassLoader().getResource(className.replace('.', '/') + ".class") == null) {
                        missing.add(file + " -> " + className);
                    }
                }
            }
        }

        assertFalse(checked.isEmpty(), "no class names found by string in " + SOURCE_ROOTS
                + "; the TribulationAPI client accessors should match");
        assertTrue(missing.isEmpty(), "sources name classes by string that are not compiled: " + missing);
    }

    /**
     * An entrypoint is either a class name string or an object whose {@code value} is the class
     * name; a {@code Class::member} reference resolves to its class.
     */
    private static String entrypointClass(JsonElement declared) {
        String value = declared.isJsonObject()
                ? declared.getAsJsonObject().get("value").getAsString()
                : declared.getAsString();
        int member = value.indexOf("::");
        return member < 0 ? value : value.substring(0, member);
    }
}
