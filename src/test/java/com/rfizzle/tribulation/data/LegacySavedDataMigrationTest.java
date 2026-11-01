// Tier: 2 (fabric-loader-junit — target paths resolve through Identifier and DimensionType)
package com.rfizzle.tribulation.data;

import com.rfizzle.tribulation.data.LegacySavedDataMigration.Outcome;
import com.rfizzle.tribulation.data.LegacySavedDataMigration.Result;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacySavedDataMigrationTest {

    private static final byte[] LEGACY_BYTES = {1, 2, 3, 4};
    private static final byte[] CURRENT_BYTES = {9, 8, 7};

    @TempDir
    Path world;

    @BeforeAll
    static void bootstrap() {
        // Level.OVERWORLD's class init touches the registries.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void paths_matchTheOneTwentyOneLayoutAndTheCurrentStorageLayout() {
        assertEquals(world.resolve("data/tribulation_players.dat"),
                LegacySavedDataMigration.legacyFile(world, PlayerDifficultyState.STORAGE_KEY));
        assertEquals(world.resolve("dimensions/minecraft/overworld/data/tribulation/tribulation_players.dat"),
                LegacySavedDataMigration.targetFile(world, PlayerDifficultyState.STORAGE_KEY));
        assertEquals(world.resolve("dimensions/minecraft/overworld/data/tribulation/tribulation_blood_moon.dat"),
                LegacySavedDataMigration.targetFile(world, BloodMoonState.STORAGE_KEY));
    }

    @Test
    void migrateWorld_movesEveryLegacyFileWithItsBytesIntact() throws IOException {
        write(LegacySavedDataMigration.legacyFile(world, PlayerDifficultyState.STORAGE_KEY), LEGACY_BYTES);
        write(LegacySavedDataMigration.legacyFile(world, BloodMoonState.STORAGE_KEY), CURRENT_BYTES);

        List<Result> results = LegacySavedDataMigration.migrateWorld(world);

        assertEquals(List.of(Outcome.MOVED, Outcome.MOVED), results.stream().map(Result::outcome).toList());
        assertArrayEquals(LEGACY_BYTES,
                Files.readAllBytes(LegacySavedDataMigration.targetFile(world, PlayerDifficultyState.STORAGE_KEY)));
        assertArrayEquals(CURRENT_BYTES,
                Files.readAllBytes(LegacySavedDataMigration.targetFile(world, BloodMoonState.STORAGE_KEY)));
        assertFalse(Files.exists(LegacySavedDataMigration.legacyFile(world, PlayerDifficultyState.STORAGE_KEY)));
        assertFalse(Files.exists(LegacySavedDataMigration.legacyFile(world, BloodMoonState.STORAGE_KEY)));
    }

    @Test
    void migrateWorld_secondRunIsANoOp() throws IOException {
        write(LegacySavedDataMigration.legacyFile(world, PlayerDifficultyState.STORAGE_KEY), LEGACY_BYTES);
        LegacySavedDataMigration.migrateWorld(world);

        List<Result> results = LegacySavedDataMigration.migrateWorld(world);

        assertEquals(List.of(Outcome.NO_LEGACY_FILE, Outcome.NO_LEGACY_FILE),
                results.stream().map(Result::outcome).toList());
        assertArrayEquals(LEGACY_BYTES,
                Files.readAllBytes(LegacySavedDataMigration.targetFile(world, PlayerDifficultyState.STORAGE_KEY)));
    }

    @Test
    void migrateWorld_freshWorldCreatesNothing() {
        List<Result> results = LegacySavedDataMigration.migrateWorld(world);

        assertEquals(List.of(Outcome.NO_LEGACY_FILE, Outcome.NO_LEGACY_FILE),
                results.stream().map(Result::outcome).toList());
        assertFalse(Files.exists(world.resolve("dimensions")));
    }

    @Test
    void migrate_neverOverwritesCurrentData() throws IOException {
        Path legacy = LegacySavedDataMigration.legacyFile(world, PlayerDifficultyState.STORAGE_KEY);
        Path target = LegacySavedDataMigration.targetFile(world, PlayerDifficultyState.STORAGE_KEY);
        write(legacy, LEGACY_BYTES);
        write(target, CURRENT_BYTES);

        Result result = LegacySavedDataMigration.migrate(legacy, target);

        assertEquals(Outcome.TARGET_EXISTS, result.outcome());
        assertArrayEquals(LEGACY_BYTES, Files.readAllBytes(legacy));
        assertArrayEquals(CURRENT_BYTES, Files.readAllBytes(target));
    }

    @Test
    void migrate_failureLeavesTheLegacyFileInPlace() throws IOException {
        Path legacy = LegacySavedDataMigration.legacyFile(world, PlayerDifficultyState.STORAGE_KEY);
        Path target = LegacySavedDataMigration.targetFile(world, PlayerDifficultyState.STORAGE_KEY);
        write(legacy, LEGACY_BYTES);
        // A regular file where the target directory belongs makes the move impossible.
        write(target.getParent(), CURRENT_BYTES);

        Result result = LegacySavedDataMigration.migrate(legacy, target);

        assertEquals(Outcome.FAILED, result.outcome());
        assertTrue(result.error() != null && !result.error().isBlank());
        assertArrayEquals(LEGACY_BYTES, Files.readAllBytes(legacy));
    }

    private static void write(Path file, byte[] bytes) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
    }
}
