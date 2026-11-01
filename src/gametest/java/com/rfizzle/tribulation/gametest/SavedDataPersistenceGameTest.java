// Tier: 3 (Fabric Gametest)
package com.rfizzle.tribulation.gametest;

import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.data.BloodMoonState;
import com.rfizzle.tribulation.data.PlayerDifficultyState;
import com.rfizzle.tribulation.data.TribulationAttachments;
import com.rfizzle.tribulation.scaling.TierManager;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import java.nio.file.Path;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.UUID;

/**
 * End-to-end persistence coverage for the two {@link DimensionDataStorage}-backed
 * states through the real cold-boot read path. {@code PlayerDifficultyStateTest}
 * and {@code BloodMoonStateTest} cover the in-memory {@code save}/{@code load} NBT
 * round-trip, but that path never touches {@link DimensionDataStorage} — the
 * storage layer that runs the datafixer and reads the {@code .dat} off disk. These
 * tests flush the live state to disk, then re-read it through a <em>fresh</em>
 * storage over the same folder (a new {@link DimensionDataStorage} with an empty
 * cache reads from disk exactly as a restarted server does) and assert the values
 * survive.
 *
 * <p>These guard that the factories carry a non-null {@code DataFixTypes}: vanilla
 * {@code DimensionDataStorage} invokes {@code DataFixTypes.update(...)} on read,
 * and while Fabric API's object-builder module currently null-guards that call, a
 * real {@code SAVED_DATA_*} constant is the self-sufficient form these tests
 * exercise end to end.
 *
 * <p>A separate concern also lives here: {@code scaledTier_clampsOutOfRangeTierOnNbtDecode}
 * round-trips a mob's {@code SCALED_TIER} attachment through entity NBT — a path that
 * never touches {@link DimensionDataStorage} — to prove the attachment codec's
 * clamping {@code xmap} coerces a corrupt tier into range on decode. The pure clamp
 * logic is unit-tested in {@code TierManagerTest}; this proves the codec is wired.
 */
public class SavedDataPersistenceGameTest {

    private static final BlockPos SPAWN = new BlockPos(1, 2, 1);

    /** Fresh storage over the overworld's data folder — the real boot-time read path. */
    private static SavedDataStorage freshOverworldStorage(MinecraftServer server) {
        Path dataDir = DimensionType.getStorageFolder(Level.OVERWORLD, server.getWorldPath(LevelResource.ROOT))
                .resolve("data");
        return new SavedDataStorage(dataDir, server.getFixerUpper(), server.registryAccess());
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void playerDifficultyState_survivesStorageReload(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        // A throwaway UUID so we neither depend on nor disturb any real player.
        UUID uuid = UUID.randomUUID();
        // Stay under any sane maxLevel so setLevel never clamps and the assert
        // is independent of the server's configured cap.
        int target = Math.min(137, Tribulation.getConfig().general.maxLevel);

        PlayerDifficultyState live = PlayerDifficultyState.getOrCreate(server);
        try {
            live.setLevel(uuid, target, Tribulation.getConfig().general.maxLevel);

            // Flush the dirty state to disk, then re-read through a cold storage
            // instance — the datafixer + disk read a restarted server runs at boot.
            server.overworld().getDataStorage().saveAndJoin();
            PlayerDifficultyState reloaded =
                    freshOverworldStorage(server).computeIfAbsent(PlayerDifficultyState.TYPE);

            helper.assertValueEqual(reloaded.getLevel(uuid), target,
                    "player level survives a disk round-trip");
            helper.succeed();
        } finally {
            // Don't leave a stray entry in the shared live singleton.
            live.reset(uuid);
        }
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void bloodMoonState_survivesStorageReload(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();

        BloodMoonState live = BloodMoonState.getOrCreate(server);
        boolean savedActive = live.isActive();
        long savedRolled = live.getLastRolledDay();
        try {
            live.setActive(true);
            live.markRolled(42L);

            server.overworld().getDataStorage().saveAndJoin();
            BloodMoonState reloaded =
                    freshOverworldStorage(server).computeIfAbsent(BloodMoonState.TYPE);

            helper.assertTrue(reloaded.isActive(),
                    "active Blood Moon survives a disk round-trip");
            helper.assertValueEqual(reloaded.getLastRolledDay(), 42L,
                    "last-rolled day survives a disk round-trip");
            helper.succeed();
        } finally {
            // Restore the shared singleton so other gametests see a clean slate.
            live.setActive(savedActive);
            live.markRolled(savedRolled);
        }
    }

    /**
     * The {@code SCALED_TIER} attachment codec must clamp an out-of-range tier from
     * corrupt entity NBT on decode — a too-large tier would otherwise let
     * {@code AbilityManager} apply every ability. {@code setAttached} writes memory
     * directly (bypassing the codec, exactly the shape of a hand-edited save); the
     * clamp lives on the persistent codec's <em>decode</em>, so the round-trip
     * through NBT is what proves the xmap is wired and fires. Unit tests cover the
     * pure {@link TierManager#clampTier} logic; this proves the wiring.
     */
    @GameTest(structure = "tribulation:empty_3x3")
    public void scaledTier_clampsOutOfRangeTierOnNbtDecode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Mob source = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, SPAWN);

        // Too-large tier clamps down to MAX_TIER on decode.
        source.setAttached(TribulationAttachments.SCALED_TIER, 999);
        int reloadedTier = decodeTierThroughNbt(level, source);
        helper.assertValueEqual(reloadedTier, TierManager.MAX_TIER,
                "out-of-range scaled tier clamps to MAX_TIER on NBT decode");

        // Negative tier clamps up to MIN_TIER on decode.
        source.setAttached(TribulationAttachments.SCALED_TIER, -7);
        int reloadedNegTier = decodeTierThroughNbt(level, source);
        helper.assertValueEqual(reloadedNegTier, TierManager.MIN_TIER,
                "negative scaled tier clamps to MIN_TIER on NBT decode");

        helper.succeed();
    }

    /**
     * Encode {@code source}'s attachments to NBT (identity on write) and decode
     * them into a fresh, unadded entity — the codec's decode path a chunk reload
     * runs — returning the clamped {@code SCALED_TIER}.
     */
    private static int decodeTierThroughNbt(ServerLevel level, Mob source) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        source.saveWithoutId(output);
        CompoundTag tag = output.buildResult();
        Zombie reloaded = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.LOAD);
        if (reloaded == null) {
            throw new IllegalStateException("could not create a zombie to decode into");
        }
        try {
            reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            return reloaded.getAttachedOrThrow(TribulationAttachments.SCALED_TIER);
        } finally {
            reloaded.discard();
        }
    }
}
