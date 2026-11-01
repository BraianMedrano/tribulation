// Tier: 3 (Fabric Gametest)
package com.rfizzle.tribulation.gametest;

import com.rfizzle.concord.gametest.MockPlayers;
import com.rfizzle.tribulation.Tribulation;
import com.rfizzle.tribulation.config.TribulationConfig;
import com.rfizzle.tribulation.data.PlayerDifficultyState;
import com.rfizzle.tribulation.event.RaidScalingHandler;
import com.rfizzle.tribulation.event.WeaponEquipmentHandler;
import com.rfizzle.tribulation.scaling.TierManager;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * End-to-end coverage of {@link RaidScalingHandler} (patrol-size scaling) and
 * the raider-equipment path that raid/patrol mobs share with every other scaled
 * spawn.
 *
 * <p>The patrol test drives {@link RaidScalingHandler#spawnExtraMembers} with an
 * explicit level so it never has to fight the {@code ENTITY_LOAD}/{@code
 * isPatrolLeader} ordering; it polls (the core defers the add by a tick).
 *
 * <p>Gametest structures are packed only a few blocks apart, so a plain
 * area scan would catch the neighbouring test's pillagers. Each test gives its
 * captain a unique patrol target (its own block position) — which the spawned
 * extras inherit — and counts only pillagers carrying that target, isolating
 * its own spawns from any concurrent test.
 */
public class RaidScalingGameTest {

    private static final BlockPos CAPTAIN_POS = new BlockPos(1, 2, 1);

    /** Count this captain's own extras (matched by inherited patrol target). */
    private static int countOwnExtras(ServerLevel world, Pillager captain, BlockPos patrolTarget) {
        List<Pillager> nearby = world.getEntitiesOfClass(
                Pillager.class, captain.getBoundingBox().inflate(16.0));
        int extras = 0;
        for (Pillager p : nearby) {
            if (p != captain && patrolTarget.equals(p.getPatrolTarget())) extras++;
        }
        return extras;
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void patrolSizeScalesWithTier(GameTestHelper helper) {
        ServerLevel world = helper.getLevel();

        TribulationConfig cfg = new TribulationConfig();
        cfg.raidScaling.enabled = true;
        cfg.raidScaling.patrolBonusRate = 1; // +1 member per tier

        int playerLevel = cfg.tiers.tier3; // tier 3
        int tier = TierManager.getTier(playerLevel, cfg.tiers);
        int expectedExtra = cfg.raidScaling.extraPatrolMembers(tier);
        helper.assertTrue(expectedExtra > 0, "Test setup must yield at least one extra member");

        Pillager captain = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, CAPTAIN_POS);
        captain.setPatrolLeader(true);
        // Unique target so the extras (which inherit it) can be told apart from
        // any concurrent test's patrol members.
        BlockPos target = captain.blockPosition();
        captain.setPatrolTarget(target);

        RaidScalingHandler.spawnExtraMembers(captain, world, playerLevel, cfg);

        helper.succeedWhen(() -> {
            int extras = countOwnExtras(world, captain, target);
            if (extras != expectedExtra) {
                helper.fail("Expected " + expectedExtra + " extra patrol members, found " + extras);
            }
        });
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void patrolSizeUnchangedWhenDisabled(GameTestHelper helper) {
        ServerLevel world = helper.getLevel();

        TribulationConfig cfg = new TribulationConfig();
        cfg.raidScaling.enabled = false;
        cfg.raidScaling.patrolBonusRate = 1;

        Pillager captain = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, CAPTAIN_POS);
        captain.setPatrolLeader(true);
        BlockPos target = captain.blockPosition();
        captain.setPatrolTarget(target);

        RaidScalingHandler.spawnExtraMembers(captain, world, cfg.tiers.tier5, cfg);

        // Give the deferred task a window to (not) run, then assert no extras.
        helper.runAfterDelay(3, () -> {
            int extras = countOwnExtras(world, captain, target);
            if (extras != 0) {
                helper.fail("Disabled raid scaling must not add patrol members, found " + extras);
            }
            helper.succeed();
        });
    }

    // ---- extra raid waves (RaidsMixin + RaidScalingMixin, end to end) ----

    /** Extra waves granted at or above the threshold; distinct from the default 1. */
    private static final int EXTRA_WAVES = 2;

    /**
     * Narrow detection radius used while a raid is created, so only this test's
     * player is folded in and never a mock player from a neighbouring structure.
     */
    private static final double ISOLATED_DETECTION_RANGE = 2.0;

    /** Vanilla {@code Raid.getNumGroups} table, before any extra waves. */
    private static int vanillaWaves(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> 0;
            case EASY -> 3;
            case NORMAL -> 5;
            case HARD -> 7;
        };
    }

    /**
     * Starts a live raid through {@link Raids#createOrExtendRaid} (the path a Raid
     * Omen takes), reads the wave count frozen into the raid, then stops it so the
     * next creation builds a fresh raid instead of extending this one.
     */
    private static int createRaidAndReadWaves(GameTestHelper helper, ServerPlayer player, BlockPos center) {
        Raid raid = helper.getLevel().getRaids().createOrExtendRaid(player, center);
        helper.assertTrue(raid != null, "createOrExtendRaid must start a raid in the test level");
        try {
            CompoundTag saved = (CompoundTag) Raid.MAP_CODEC.codec()
                    .encodeStart(NbtOps.INSTANCE, raid).getOrThrow();
            return saved.getIntOr("group_count", -1);
        } finally {
            raid.stop();
        }
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void raidWaveCountFollowsTargetedPlayerTier(GameTestHelper helper) {
        ServerLevel world = helper.getLevel();
        BlockPos center = helper.absolutePos(CAPTAIN_POS);
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);

        // The mixins read the live config; mutate it only inside this synchronous
        // body so no other test observes the change.
        TribulationConfig live = Tribulation.getConfig();
        PlayerDifficultyState state = PlayerDifficultyState.getOrCreate(world.getServer());
        boolean savedEnabled = live.raidScaling.enabled;
        int savedThreshold = live.raidScaling.extraWaveTierThreshold;
        int savedCount = live.raidScaling.extraWaveCount;
        double savedRange = live.general.mobDetectionRange;
        int savedLevel = state.getLevel(player.getUUID());
        try {
            live.raidScaling.enabled = true;
            live.raidScaling.extraWaveTierThreshold = 4;
            live.raidScaling.extraWaveCount = EXTRA_WAVES;
            live.general.mobDetectionRange = ISOLATED_DETECTION_RANGE;
            int vanilla = vanillaWaves(world.getDifficulty());

            state.setLevel(player.getUUID(), 0, live.general.maxLevel);
            int lowTier = createRaidAndReadWaves(helper, player, center);
            helper.assertValueEqual(lowTier, vanilla, "waves for a tier-0 player");

            state.setLevel(player.getUUID(), live.tiers.tier5, live.general.maxLevel);
            int highTier = createRaidAndReadWaves(helper, player, center);
            helper.assertValueEqual(highTier, vanilla + EXTRA_WAVES, "waves for a tier-5 player");

            live.raidScaling.enabled = false;
            int disabled = createRaidAndReadWaves(helper, player, center);
            helper.assertValueEqual(disabled, vanilla, "waves for a tier-5 player with raid scaling off");
        } finally {
            live.raidScaling.enabled = savedEnabled;
            live.raidScaling.extraWaveTierThreshold = savedThreshold;
            live.raidScaling.extraWaveCount = savedCount;
            live.general.mobDetectionRange = savedRange;
            state.setLevel(player.getUUID(), savedLevel, live.general.maxLevel);
            MockPlayers.retire(player);
        }
        helper.succeed();
    }

    /**
     * A raid restored from disk (server restart) has not captured its extra waves yet,
     * and its first {@code getNumGroups} call happens inside {@code spawnGroup}. That call
     * must still see the level, as it did through {@code Raid.level} on 1.21.1; otherwise
     * the raid would memoize zero extra waves and disagree with its saved wave count.
     */
    @GameTest(structure = "tribulation:empty_3x3")
    public void restoredRaidKeepsTierWavesWhenSpawningAGroup(GameTestHelper helper) throws ReflectiveOperationException {
        ServerLevel world = helper.getLevel();
        BlockPos center = helper.absolutePos(CAPTAIN_POS);
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);
        player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);

        TribulationConfig live = Tribulation.getConfig();
        PlayerDifficultyState state = PlayerDifficultyState.getOrCreate(world.getServer());
        boolean savedEnabled = live.raidScaling.enabled;
        int savedThreshold = live.raidScaling.extraWaveTierThreshold;
        int savedCount = live.raidScaling.extraWaveCount;
        double savedRange = live.general.mobDetectionRange;
        int savedLevel = state.getLevel(player.getUUID());
        Raid restored = null;
        try {
            live.raidScaling.enabled = true;
            live.raidScaling.extraWaveTierThreshold = 4;
            live.raidScaling.extraWaveCount = EXTRA_WAVES;
            live.general.mobDetectionRange = ISOLATED_DETECTION_RANGE;
            state.setLevel(player.getUUID(), live.tiers.tier5, live.general.maxLevel);
            int expected = vanillaWaves(world.getDifficulty()) + EXTRA_WAVES;

            Raid raid = world.getRaids().createOrExtendRaid(player, center);
            helper.assertTrue(raid != null, "createOrExtendRaid must start a raid in the test level");
            CompoundTag saved;
            try {
                saved = (CompoundTag) Raid.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, raid).getOrThrow();
            } finally {
                raid.stop();
            }
            helper.assertValueEqual(saved.getIntOr("group_count", -1), expected, "saved wave count");
            // spawnGroup only calls getNumGroups for a ravager, and wave 3 always has one, so
            // resume the raid after wave 2 the way a saved mid-raid world would.
            saved.putInt("groups_spawned", 2);

            restored = Raid.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
            Method spawnGroup = Raid.class.getDeclaredMethod("spawnGroup", ServerLevel.class, BlockPos.class);
            spawnGroup.setAccessible(true);
            spawnGroup.invoke(restored, world, center);

            helper.assertValueEqual(restored.getNumGroups(world.getDifficulty()), expected,
                    "wave count a restored raid sees after spawning a group");
        } finally {
            if (restored != null) {
                restored.getAllRaiders().forEach(Entity::discard);
                restored.stop();
            }
            live.raidScaling.enabled = savedEnabled;
            live.raidScaling.extraWaveTierThreshold = savedThreshold;
            live.raidScaling.extraWaveCount = savedCount;
            live.general.mobDetectionRange = savedRange;
            state.setLevel(player.getUUID(), savedLevel, live.general.maxLevel);
            MockPlayers.retire(player);
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void raiderEquipmentApplied(GameTestHelper helper) {
        // Raiders (pillager/vindicator are AbstractIllager) flow through the same
        // weapon-equipment path as every other scaled mob; validate it end-to-end.
        Mob mob = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, CAPTAIN_POS);
        mob.entityTags().remove(WeaponEquipmentHandler.PROCESSED_TAG);
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        TribulationConfig cfg = new TribulationConfig();
        cfg.weaponEquipment.enabled = true;
        TribulationConfig.WeaponTier tier5 = cfg.weaponEquipment.tiers.get("tier5");
        tier5.wearChancePercent = 100;

        Set<Item> allowed = new HashSet<>();
        for (var e : tier5.materialWeights.entrySet()) {
            if (e.getValue() <= 0) continue;
            WeaponEquipmentHandler.WeaponMaterial mat = WeaponEquipmentHandler.WeaponMaterial.fromKey(e.getKey());
            if (mat != null) {
                allowed.add(mat.getSword());
                allowed.add(mat.getAxe());
            }
        }

        WeaponEquipmentHandler.processWeapon(mob, 5, cfg);

        helper.succeedWhen(() -> {
            ItemStack mainHand = mob.getMainHandItem();
            if (mainHand.isEmpty()) {
                helper.fail("Raider should have a tier weapon equipped at tier 5 with 100% wear chance");
            }
            if (!allowed.contains(mainHand.getItem())) {
                helper.fail("Raider equipped out-of-pool weapon " + mainHand.getItem());
            }
        });
    }
}
