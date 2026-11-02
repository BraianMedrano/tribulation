package com.rfizzle.tribulation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.reflect.AccessFlag;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard that every class the mod names by string, in the shipped manifest's entrypoints or in a
 * reflective lookup in its compiled code, is actually compiled.
 *
 * <p>The optional integrations are wired in two places kept in sync by hand: the entrypoint in
 * {@code fabric.mod.json} and the source set that compiles the adapter class. If a class is
 * excluded from compilation (or renamed, or deleted) while its entrypoint stays, nothing fails
 * until a player installs that optional mod, and then Fabric Loader crashes at startup with a
 * {@code ClassNotFoundException}. This test fails the build instead.
 *
 * <p>Classes are looked up as resources or read as class files rather than loaded: an adapter
 * references the optional mod's API, which is compile-only and absent from the test runtime, so
 * loading it would fail for the wrong reason. The test classpath carries both the {@code main}
 * and {@code client} outputs, which covers every entrypoint environment.
 *
 * <p>The same failure hides behind the reflective bridges from the common source set into the
 * client one (the {@code TribulationAPI} client accessors, the Jade tooltip component): a stale
 * name there degrades silently at runtime. So every string constant in the compiled {@code main}
 * and {@code client} classes that names a mod class must resolve, and the Jade bridge's target
 * method signature is checked too. Scanning compiled classes, not sources, skips the adapters the
 * build excludes (they are never compiled, so they can never run).
 */
class ManifestEntrypointsCompiledTest {
    private static final Path SHIPPED_MANIFEST = Path.of("src/main/resources/fabric.mod.json");
    private static final List<Path> COMPILED_ROOTS =
            List.of(Path.of("build/classes/java/main"), Path.of("build/classes/java/client"));
    /** A fully qualified mod class name: lower-case package segments, then a capitalized class. */
    private static final Pattern MOD_CLASS_NAME =
            Pattern.compile("com\\.rfizzle\\.tribulation\\.(?:[a-z0-9_]+\\.)*[A-Z][A-Za-z0-9_$]*");

    private static final String JADE_PLUGIN = "com/rfizzle/tribulation/compat/jade/JadeTribulationPlugin.class";
    private static final String JADE_CLIENT_REGISTRATION = "Lsnownee/jade/api/IWailaClientRegistration;";

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
                if (!isCompiled(className)) {
                    missing.add(entry.getKey() + " -> " + className);
                }
            }
        }

        assertFalse(checked.isEmpty(), "no entrypoints found in " + SHIPPED_MANIFEST);
        assertTrue(missing.isEmpty(), "entrypoints in " + SHIPPED_MANIFEST
                + " name classes that are not compiled (excluded from a source set, renamed, or deleted): " + missing);
    }

    @Test
    void everyClassNamedByStringInCompiledCodeIsCompiled() throws IOException {
        List<String> checked = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Path root : COMPILED_ROOTS) {
            assertTrue(Files.isDirectory(root), "compiled classes not found at " + root);
            List<Path> classFiles;
            try (Stream<Path> tree = Files.walk(root)) {
                classFiles = tree.filter(p -> p.toString().endsWith(".class")).toList();
            }
            for (Path classFile : classFiles) {
                ClassModel model = ClassFile.of().parse(classFile);
                for (PoolEntry entry : model.constantPool()) {
                    if (entry instanceof StringEntry string && MOD_CLASS_NAME.matcher(string.stringValue()).matches()) {
                        String className = string.stringValue();
                        checked.add(className);
                        if (!isCompiled(className)) {
                            missing.add(root.relativize(classFile) + " -> " + className);
                        }
                    }
                }
            }
        }

        assertFalse(checked.isEmpty(), "no class names found as string constants under " + COMPILED_ROOTS
                + "; the TribulationAPI client accessors should match");
        assertTrue(missing.isEmpty(), "compiled code names classes by string that are not compiled: " + missing);
    }

    @Test
    void jadeClientBridgeTargetsPublicStaticRegister() throws IOException {
        ClassModel plugin = readClass(JADE_PLUGIN);
        String componentName = stringConstant(plugin, "CLIENT_COMPONENT");
        String methodName = stringConstant(plugin, "CLIENT_REGISTER_METHOD");

        ClassModel component = readClass(componentName.replace('.', '/') + ".class");
        String descriptor = "(" + JADE_CLIENT_REGISTRATION + ")V";
        MethodModel target = component.methods().stream()
                .filter(m -> m.methodName().equalsString(methodName) && m.methodType().equalsString(descriptor))
                .findFirst()
                .orElse(null);

        assertNotNull(target, componentName + " has no method " + methodName + descriptor
                + ", which JadeTribulationPlugin#registerClient looks up by name; the Jade tooltip would silently vanish");
        assertTrue(target.flags().has(AccessFlag.PUBLIC) && target.flags().has(AccessFlag.STATIC),
                componentName + "#" + methodName + " must be public static for MethodHandles.publicLookup().findStatic");
    }

    private boolean isCompiled(String className) {
        return getClass().getClassLoader().getResource(className.replace('.', '/') + ".class") != null;
    }

    private ClassModel readClass(String resource) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, resource + " is not on the test classpath");
            return ClassFile.of().parse(in.readAllBytes());
        }
    }

    /** The compile-time value of a {@code static final String} field, read from its ConstantValue attribute. */
    private static String stringConstant(ClassModel model, String fieldName) {
        FieldModel field = model.fields().stream()
                .filter(f -> f.fieldName().equalsString(fieldName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(model.thisClass().asInternalName() + " has no field " + fieldName));
        return field.findAttribute(Attributes.constantValue())
                .map(attribute -> ((StringEntry) attribute.constant()).stringValue())
                .orElseThrow(() -> new AssertionError(fieldName + " is not a compile-time String constant"));
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
