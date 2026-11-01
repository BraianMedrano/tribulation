package com.rfizzle.tribulation.data;

import com.rfizzle.tribulation.Tribulation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Moves Tribulation's saved data from the 1.21.x location to the 26.x one.
 *
 * <p>Up to 1.21.x every {@code SavedData} lived in {@code <world>/data/<key>.dat}.
 * Since 26.1 the overworld storage resolves a {@code SavedDataType} id against
 * {@code <world>/dimensions/minecraft/overworld/data/}, so ours now live in
 * {@code .../data/tribulation/<key>.dat}. Vanilla's world upgrade
 * ({@code DimensionStorageFileFix}) only moves the vanilla files it names, so an
 * upgraded world would otherwise start with every player back at level 0.
 *
 * <p>The NBT layout did not change, so the file is moved as is. It runs on
 * {@code SERVER_STARTING}, which Fabric fires before {@code initServer()} loads
 * the levels and therefore before anything reads these states. An existing
 * target is never overwritten, and any failure leaves the legacy file in place.
 */
public final class LegacySavedDataMigration {

    /** Every {@code SavedData} key Tribulation wrote under {@code <world>/data/} in 1.21.x. */
    static final List<String> STORAGE_KEYS = List.of(
            PlayerDifficultyState.STORAGE_KEY,
            BloodMoonState.STORAGE_KEY
    );

    public enum Outcome { MOVED, NO_LEGACY_FILE, TARGET_EXISTS, FAILED }

    public record Result(Path from, Path to, Outcome outcome, String error) {}

    private LegacySavedDataMigration() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(LegacySavedDataMigration::run);
    }

    private static void run(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT).normalize();
        for (Result result : migrateWorld(worldRoot)) {
            String message = message(result);
            switch (result.outcome()) {
                case MOVED -> Tribulation.LOGGER.info(message);
                case TARGET_EXISTS -> Tribulation.LOGGER.warn(message);
                case FAILED -> Tribulation.LOGGER.error(message);
                case NO_LEGACY_FILE -> { }
            }
        }
    }

    /**
     * The log line for a result, or {@code null} when there is nothing to report. After a
     * failed move the server goes on to write a fresh file at the target, so later starts
     * report {@link Outcome#TARGET_EXISTS}; both messages therefore say the target has to be
     * replaced, not just that the legacy file has to be moved.
     */
    static String message(Result result) {
        return switch (result.outcome()) {
            case MOVED -> "Moved legacy Tribulation data " + result.from() + " to " + result.to();
            case TARGET_EXISTS -> "Legacy Tribulation data " + result.from() + " was not moved because "
                    + result.to() + " already exists, so the existing file is used and the legacy one is "
                    + "left untouched. If the legacy file holds this world's real progress (for example, "
                    + "the world was opened once by a build that could not move it), stop the server and "
                    + "replace " + result.to() + " with it.";
            case FAILED -> "Could not move legacy Tribulation data " + result.from() + " to " + result.to()
                    + " (" + result.error() + "). This start uses empty Tribulation data and will save it "
                    + "to " + result.to() + ". To keep this world's progress, stop the server and replace "
                    + result.to() + " with " + result.from() + ".";
            case NO_LEGACY_FILE -> null;
        };
    }

    static List<Result> migrateWorld(Path worldRoot) {
        List<Result> results = new ArrayList<>(STORAGE_KEYS.size());
        for (String key : STORAGE_KEYS) {
            results.add(migrate(legacyFile(worldRoot, key), targetFile(worldRoot, key)));
        }
        return results;
    }

    static Path legacyFile(Path worldRoot, String key) {
        return worldRoot.resolve(LevelResource.DATA.id()).resolve(key + ".dat");
    }

    /** Same resolution as the overworld's {@code SavedDataStorage.getDataFile}. */
    static Path targetFile(Path worldRoot, String key) {
        Path overworldData = DimensionType.getStorageFolder(Level.OVERWORLD, worldRoot).resolve("data");
        return Tribulation.id(key).withSuffix(".dat").resolveAgainst(overworldData);
    }

    static Result migrate(Path legacy, Path target) {
        if (!Files.isRegularFile(legacy)) {
            return new Result(legacy, target, Outcome.NO_LEGACY_FILE, null);
        }
        if (Files.exists(target)) {
            return new Result(legacy, target, Outcome.TARGET_EXISTS, null);
        }
        try {
            Files.createDirectories(target.getParent());
            // No REPLACE_EXISTING: a target that appears meanwhile fails the move instead of being lost.
            Files.move(legacy, target);
            return new Result(legacy, target, Outcome.MOVED, null);
        } catch (IOException | RuntimeException e) {
            return new Result(legacy, target, Outcome.FAILED, e.toString());
        }
    }
}
