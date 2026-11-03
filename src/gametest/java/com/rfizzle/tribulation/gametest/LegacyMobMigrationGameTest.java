package com.rfizzle.tribulation.gametest;

import com.rfizzle.tribulation.ability.AbilityManager;
import com.rfizzle.tribulation.config.TribulationConfig;
import com.rfizzle.tribulation.data.TribulationAttachments;
import com.rfizzle.tribulation.event.MobScalingHandler;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import java.util.List;

/**
 * Mobs saved by older builds keep the state those builds gave them, because
 * scaling runs once per mob and {@link MobScalingHandler#PROCESSED_TAG} skips
 * it on every later load. These tests rebuild that saved state, add the mob to
 * the level so {@code ENTITY_LOAD} fires as it does on a chunk load, and check
 * the load path brings the mob up to current behavior.
 */
public class LegacyMobMigrationGameTest {

    private static final BlockPos SPAWN = new BlockPos(1, 2, 1);

    /** Create a mob carrying the processed tag, as a reloaded mob does. */
    private static <T extends Mob> T legacyMob(GameTestHelper helper, EntityType<T> type) {
        T mob = type.create(helper.getLevel(), EntitySpawnReason.LOAD);
        if (mob == null) throw new IllegalStateException("could not create " + type);
        BlockPos pos = helper.absolutePos(SPAWN);
        mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        mob.setNoAi(true);
        mob.addTag(MobScalingHandler.PROCESSED_TAG);
        return mob;
    }

    private static MobEffectInstance infiniteFireResistance() {
        return new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                MobEffectInstance.INFINITE_DURATION, 0, false, false);
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void legacyFlameSkeleton_migratedOnLoad(GameTestHelper helper) {
        Mob skeleton = legacyMob(helper, EntityTypes.SKELETON);
        // The old flame-arrows ability: infinite Fire Resistance and a skeleton
        // set alight for good, with no tag for the arrow mixin to read.
        skeleton.addEffect(infiniteFireResistance());
        skeleton.setRemainingFireTicks(Integer.MAX_VALUE);

        helper.getLevel().addFreshEntity(skeleton);

        helper.assertTrue(skeleton.entityTags().contains(AbilityManager.TAG_FLAME_ARROWS),
                "legacy flame skeleton must gain the flame-arrows tag");
        helper.assertFalse(skeleton.hasEffect(MobEffects.FIRE_RESISTANCE),
                "legacy flame skeleton must lose its infinite Fire Resistance");
        helper.assertValueEqual(skeleton.getRemainingFireTicks() > 0, false,
                "legacy flame skeleton must stop burning");
        helper.assertTrue(skeleton.entityTags().contains(MobScalingHandler.MIGRATED_TAG),
                "a migrated mob must be marked so the migration never runs on it again");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void processedMobsWithoutLegacyState_untouchedOnLoad(GameTestHelper helper) {
        // A processed skeleton below tier 4 never had the old ability.
        Mob plain = legacyMob(helper, EntityTypes.SKELETON);
        // A tier-3 zoglin keeps infinite Fire Resistance from its own ability.
        Mob zoglin = legacyMob(helper, EntityTypes.ZOGLIN);
        zoglin.addEffect(infiniteFireResistance());
        // A finite Fire Resistance on a skeleton comes from a potion, not the ability.
        Mob potioned = legacyMob(helper, EntityTypes.SKELETON);
        potioned.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600));

        helper.getLevel().addFreshEntity(plain);
        helper.getLevel().addFreshEntity(zoglin);
        helper.getLevel().addFreshEntity(potioned);

        helper.assertFalse(plain.entityTags().contains(AbilityManager.TAG_FLAME_ARROWS),
                "a skeleton without the old ability must not gain flame arrows");
        helper.assertTrue(zoglin.hasEffect(MobEffects.FIRE_RESISTANCE),
                "a zoglin must keep its Fire Resistance ability");
        helper.assertTrue(potioned.hasEffect(MobEffects.FIRE_RESISTANCE),
                "a potion's Fire Resistance must survive the load");
        helper.assertFalse(potioned.entityTags().contains(AbilityManager.TAG_FLAME_ARROWS),
                "a potion's Fire Resistance must not grant flame arrows");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void legacyChampionName_hiddenThroughWallsOnLoad(GameTestHelper helper) {
        Mob champion = legacyMob(helper, EntityTypes.ZOMBIE);
        champion.setAttached(TribulationAttachments.CHAMPION_AFFIXES, List.of("vampiric"));
        champion.setCustomName(Component.literal("Vampiric Zombie Champion"));
        // Older builds drew the champion name always, so it showed through blocks.
        champion.setCustomNameVisible(true);

        helper.getLevel().addFreshEntity(champion);

        helper.assertFalse(champion.isCustomNameVisible(),
                "legacy champion name must no longer render through walls");
        helper.assertTrue(champion.hasCustomName(), "legacy champion keeps its name");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void nonChampionVisibleName_untouchedOnLoad(GameTestHelper helper) {
        Mob named = legacyMob(helper, EntityTypes.ZOMBIE);
        named.setCustomName(Component.literal("Steve"));
        named.setCustomNameVisible(true);

        helper.getLevel().addFreshEntity(named);

        helper.assertTrue(named.isCustomNameVisible(),
                "a visible name on a non-champion is not ours to change");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void migratedMob_laterStateSurvivesReload(GameTestHelper helper) {
        // Already migrated once; an admin then gave this skeleton infinite Fire
        // Resistance and made this champion's name always visible.
        Mob skeleton = legacyMob(helper, EntityTypes.SKELETON);
        skeleton.addTag(MobScalingHandler.MIGRATED_TAG);
        skeleton.addEffect(infiniteFireResistance());
        Mob champion = legacyMob(helper, EntityTypes.ZOMBIE);
        champion.addTag(MobScalingHandler.MIGRATED_TAG);
        champion.setAttached(TribulationAttachments.CHAMPION_AFFIXES, List.of("vampiric"));
        champion.setCustomName(Component.literal("Vampiric Zombie Champion"));
        champion.setCustomNameVisible(true);

        helper.getLevel().addFreshEntity(skeleton);
        helper.getLevel().addFreshEntity(champion);

        helper.assertTrue(skeleton.hasEffect(MobEffects.FIRE_RESISTANCE),
                "state set after migration must survive the reload");
        helper.assertFalse(skeleton.entityTags().contains(AbilityManager.TAG_FLAME_ARROWS),
                "a migrated skeleton must not be migrated again");
        helper.assertTrue(champion.isCustomNameVisible(),
                "a name made visible after migration must survive the reload");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void newlyScaledMob_markedAsMigrated(GameTestHelper helper) {
        // A mob scaled by this build has nothing to migrate, so it never should.
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, SPAWN);
        zombie.removeTag(MobScalingHandler.PROCESSED_TAG);
        zombie.removeTag(MobScalingHandler.MIGRATED_TAG);
        MobScalingHandler.applyScaling(zombie, helper.getLevel(), EntityTypes.ZOMBIE,
                BuiltInRegistries.ENTITY_TYPE.getKey(EntityTypes.ZOMBIE), 50, new TribulationConfig());

        helper.assertTrue(zombie.entityTags().contains(MobScalingHandler.PROCESSED_TAG),
                "the scaled zombie must be processed");
        helper.assertTrue(zombie.entityTags().contains(MobScalingHandler.MIGRATED_TAG),
                "a mob scaled by this build must be marked as migrated");
        helper.succeed();
    }
}
