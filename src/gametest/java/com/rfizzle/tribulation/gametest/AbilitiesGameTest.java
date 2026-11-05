package com.rfizzle.tribulation.gametest;

import com.rfizzle.tribulation.ability.AbilityManager;
import com.rfizzle.tribulation.config.TribulationConfig;
import com.rfizzle.tribulation.ability.SkeletonWeaponSwitcher;
import com.rfizzle.tribulation.data.TribulationAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.lang.reflect.Method;

/**
 * Deterministic gametest coverage for the tier abilities that hinge on live
 * registry/entity state: arrow upgrades (Skeleton flame, Stray/Bogged effects), held-crossbow
 * enchantments (Pillager), and the door-breaking difficulty relaxation
 * (Vindicator). Each ability is asserted both enabled and disabled so the
 * config toggle is proven to gate it. Projectile flight and door-pathfinding
 * timing are avoided in favor of inspecting the produced arrow / goal directly.
 */
public class AbilitiesGameTest {

    private static final BlockPos SPAWN = new BlockPos(1, 2, 1);

    // ---- Zombie: reinforcements attribute modifier (registry-driven apply) ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void zombie_reinforcementsModifierAtTier1(GameTestHelper helper) {
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(zombie, 1, "zombie", cfg);

        AttributeInstance inst = zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (inst == null || inst.getModifier(AbilityManager.abilityId("zombie_reinforcements")) == null) {
            helper.fail("Tier-1 zombie should carry the reinforcements attribute modifier");
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void zombie_reinforcementsAbsentWhenDisabled(GameTestHelper helper) {
        Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        cfg.abilities.zombieReinforcements = false;
        AbilityManager.applyAbilities(zombie, 1, "zombie", cfg);

        AttributeInstance inst = zombie.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (inst != null && inst.getModifier(AbilityManager.abilityId("zombie_reinforcements")) != null) {
            helper.fail("Disabled toggle should leave no reinforcements modifier");
        }
        helper.succeed();
    }

    // ---- Skeleton: sword switch ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void skeleton_swordSwitchDrawsSwordAndStashesBow(GameTestHelper helper) {
        Skeleton skeleton = (Skeleton) helper.spawnWithNoFreeWill(EntityTypes.SKELETON, SPAWN);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        AbilityManager.applyAbilities(skeleton, 2, "skeleton", new TribulationConfig());

        helper.assertTrue(skeleton.getMainHandItem().is(Items.STONE_SWORD),
                "Tier-2 skeleton should spawn with the sword drawn, got " + skeleton.getMainHandItem());
        ItemStack stashed = skeleton.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY);
        helper.assertTrue(stashed.is(Items.BOW), "The replaced bow should be stashed, got " + stashed);
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void skeleton_swordSwitchDisabledKeepsBow(GameTestHelper helper) {
        Skeleton skeleton = (Skeleton) helper.spawnWithNoFreeWill(EntityTypes.SKELETON, SPAWN);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        TribulationConfig cfg = new TribulationConfig();
        cfg.abilities.skeletonSwordSwitch = false;
        AbilityManager.applyAbilities(skeleton, 2, "skeleton", cfg);

        helper.assertTrue(skeleton.getMainHandItem().is(Items.BOW), "Disabled toggle should leave the bow drawn");
        helper.assertFalse(skeleton.hasAttached(TribulationAttachments.STASHED_WEAPON),
                "Disabled toggle should stash nothing");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void skeleton_swordSwitchTracksTargetDistance(GameTestHelper helper) {
        // Drives the switcher directly to pin the hysteresis band step by step;
        // skeleton_swordSwitchDuringRealAiStepKeepsGoalSetIntact covers the AI hook.
        Skeleton skeleton = (Skeleton) helper.spawnWithNoFreeWill(EntityTypes.SKELETON, SPAWN);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        AbilityManager.applyAbilities(skeleton, 2, "skeleton", new TribulationConfig());
        ArmorStand target = helper.spawn(EntityTypes.ARMOR_STAND, SPAWN);
        target.setNoGravity(true);
        skeleton.setTarget(target);

        target.setPos(skeleton.getX() + 12, skeleton.getY(), skeleton.getZ());
        SkeletonWeaponSwitcher.tick(skeleton);
        helper.assertTrue(skeleton.getMainHandItem().is(Items.BOW), "A far target should make it draw the bow");
        helper.assertTrue(skeleton.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY)
                .is(Items.STONE_SWORD), "The sword should be stashed while the bow is drawn");

        target.setPos(skeleton.getX() + 5.5, skeleton.getY(), skeleton.getZ());
        SkeletonWeaponSwitcher.tick(skeleton);
        helper.assertTrue(skeleton.getMainHandItem().is(Items.BOW), "Inside the hysteresis band the bow should stay drawn");

        target.setPos(skeleton.getX() + 1.5, skeleton.getY(), skeleton.getZ());
        SkeletonWeaponSwitcher.tick(skeleton);
        helper.assertTrue(skeleton.getMainHandItem().is(Items.STONE_SWORD), "A close target should make it draw the sword");
        helper.assertTrue(skeleton.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY)
                .is(Items.BOW), "The bow should be stashed while the sword is drawn");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void skeleton_swordSwitchDuringRealAiStepKeepsGoalSetIntact(GameTestHelper helper) {
        // Regression: swapping inside GoalSelector.tick made vanilla reassessWeaponGoal
        // mutate the goal set mid-iteration and crashed the server ("goal" is null).
        // aiStep runs the real goal selector; Peaceful despawn only happens in the
        // level tick, so stepping by hand inside the test body is safe.
        Skeleton skeleton = helper.spawn(EntityTypes.SKELETON, SPAWN);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        AbilityManager.applyAbilities(skeleton, 2, "skeleton", new TribulationConfig());
        ArmorStand target = helper.spawn(EntityTypes.ARMOR_STAND, SPAWN);
        target.setNoGravity(true);
        target.setPos(skeleton.getX() + 12, skeleton.getY(), skeleton.getZ());
        skeleton.setTarget(target);

        try {
            for (int i = 0; i < 4; i++) {
                skeleton.aiStep();
            }
        } catch (RuntimeException e) {
            helper.fail("Weapon swap during the AI step must not break the goal selector: " + e);
            return;
        }

        helper.assertTrue(skeleton.getMainHandItem().is(Items.BOW),
                "A far target should make it draw the bow during the AI step, got " + skeleton.getMainHandItem());
        helper.assertTrue(skeleton.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY)
                .is(Items.STONE_SWORD), "The sword should be stashed while the bow is drawn");
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void skeleton_stashedWeaponSurvivesReload(GameTestHelper helper) {
        Skeleton original = (Skeleton) helper.spawnWithNoFreeWill(EntityTypes.SKELETON, SPAWN);
        original.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        AbilityManager.applyAbilities(original, 2, "skeleton", new TribulationConfig());

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        original.discard();

        Skeleton reloaded = (Skeleton) helper.spawnWithNoFreeWill(EntityTypes.SKELETON, new BlockPos(2, 2, 1));
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

        ItemStack stashed = reloaded.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY);
        helper.assertTrue(stashed.is(Items.BOW), "The stashed bow should survive save/load, got " + stashed);
        helper.succeed();
    }

    // ---- Skeleton: flame arrows ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void skeleton_flameArrowsLitAtTier4(GameTestHelper helper) {
        Skeleton skeleton = (Skeleton) helper.spawnWithNoFreeWill(EntityTypes.SKELETON, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(skeleton, 4, "skeleton", cfg);

        if (!skeleton.entityTags().contains(AbilityManager.TAG_FLAME_ARROWS)) {
            helper.fail("Tier-4 skeleton should carry the flame-arrows tag");
        }
        AbstractArrow arrow = produceArrow(helper, skeleton);
        if (arrow.getRemainingFireTicks() <= 0) {
            helper.fail("Tier-4 skeleton arrow should be on fire, got fire ticks " + arrow.getRemainingFireTicks());
        }
        if (skeleton.getRemainingFireTicks() > 0) {
            helper.fail("Tier-4 skeleton itself should not burn, got fire ticks " + skeleton.getRemainingFireTicks());
        }
        if (skeleton.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            helper.fail("Tier-4 skeleton should not gain Fire Resistance from the flame-arrows ability");
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void skeleton_flameArrowsUnlitWhenDisabled(GameTestHelper helper) {
        Skeleton skeleton = (Skeleton) helper.spawnWithNoFreeWill(EntityTypes.SKELETON, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        cfg.abilities.skeletonFlameArrows = false;
        AbilityManager.applyAbilities(skeleton, 4, "skeleton", cfg);

        AbstractArrow arrow = produceArrow(helper, skeleton);
        if (arrow.getRemainingFireTicks() > 0) {
            helper.fail("Disabled toggle should leave the arrow unlit, got fire ticks " + arrow.getRemainingFireTicks());
        }
        helper.succeed();
    }

    // ---- Stray: Slowness II arrows ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void stray_slownessArrowsUpgradedAtTier2(GameTestHelper helper) {
        Stray stray = (Stray) helper.spawnWithNoFreeWill(EntityTypes.STRAY, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(stray, 2, "stray", cfg);

        int amplifier = maxArrowEffectAmplifier(helper, stray, MobEffects.SLOWNESS.value());
        if (amplifier != 1) {
            helper.fail("Tier-2 stray arrow should carry Slowness II (amp 1), got amp " + amplifier);
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void stray_slownessArrowsVanillaWhenDisabled(GameTestHelper helper) {
        Stray stray = (Stray) helper.spawnWithNoFreeWill(EntityTypes.STRAY, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        cfg.abilities.straySlownessUpgrade = false;
        AbilityManager.applyAbilities(stray, 2, "stray", cfg);

        int amplifier = maxArrowEffectAmplifier(helper, stray, MobEffects.SLOWNESS.value());
        if (amplifier != 0) {
            helper.fail("Disabled toggle should leave vanilla Slowness I (amp 0), got amp " + amplifier);
        }
        helper.succeed();
    }

    // ---- Bogged: Poison II arrows ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void bogged_poisonArrowsUpgradedAtTier2(GameTestHelper helper) {
        Bogged bogged = (Bogged) helper.spawnWithNoFreeWill(EntityTypes.BOGGED, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(bogged, 2, "bogged", cfg);

        int amplifier = maxArrowEffectAmplifier(helper, bogged, MobEffects.POISON.value());
        if (amplifier != 1) {
            helper.fail("Tier-2 bogged arrow should carry Poison II (amp 1), got amp " + amplifier);
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void bogged_poisonArrowsVanillaWhenDisabled(GameTestHelper helper) {
        Bogged bogged = (Bogged) helper.spawnWithNoFreeWill(EntityTypes.BOGGED, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        cfg.abilities.boggedPoisonUpgrade = false;
        AbilityManager.applyAbilities(bogged, 2, "bogged", cfg);

        int amplifier = maxArrowEffectAmplifier(helper, bogged, MobEffects.POISON.value());
        if (amplifier != 0) {
            helper.fail("Disabled toggle should leave vanilla Poison I (amp 0), got amp " + amplifier);
        }
        helper.succeed();
    }

    // ---- Endermite: call sleepers when hurt ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void endermite_callsSleepersWhenHurt(GameTestHelper helper) {
        helper.getLevel().getGameRules()
                .set(GameRules.MOB_GRIEFING, true, helper.getLevel().getServer());

        // An infested block one tile below the endermite reverts to its host
        // (stone) when the ability summons a silverfish from it.
        BlockPos infested = new BlockPos(1, 1, 1);
        helper.setBlock(infested, Blocks.INFESTED_STONE);

        Endermite mite = (Endermite) helper.spawnWithNoFreeWill(EntityTypes.ENDERMITE, new BlockPos(1, 2, 1));
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(mite, 2, "endermite", cfg);

        // AFTER_DAMAGE fires synchronously inside hurt(), so the summon runs now.
        mite.hurt(helper.getLevel().damageSources().generic(), 1.0f);

        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.STONE, infested));
    }

    // ---- Pillager: Quick Charge (T2) / Multishot (T4) ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void pillager_quickChargeAtTier2(GameTestHelper helper) {
        Mob pillager = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, SPAWN);
        pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(pillager, 2, "pillager", cfg);

        if (enchantLevel(helper, pillager, Enchantments.QUICK_CHARGE) <= 0) {
            helper.fail("Tier-2 pillager crossbow should have Quick Charge");
        }
        // Multishot is a tier-4 ability; it must not appear at tier 2.
        if (enchantLevel(helper, pillager, Enchantments.MULTISHOT) > 0) {
            helper.fail("Tier-2 pillager crossbow should NOT have Multishot yet");
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void pillager_multishotAtTier4(GameTestHelper helper) {
        Mob pillager = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, SPAWN);
        pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(pillager, 4, "pillager", cfg);

        if (enchantLevel(helper, pillager, Enchantments.MULTISHOT) <= 0) {
            helper.fail("Tier-4 pillager crossbow should have Multishot");
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void pillager_crossbowUnenchantedWhenDisabled(GameTestHelper helper) {
        Mob pillager = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, SPAWN);
        pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        TribulationConfig cfg = new TribulationConfig();
        cfg.abilities.pillagerQuickCharge = false;
        cfg.abilities.pillagerMultishot = false;
        AbilityManager.applyAbilities(pillager, 4, "pillager", cfg);

        if (enchantLevel(helper, pillager, Enchantments.QUICK_CHARGE) > 0
                || enchantLevel(helper, pillager, Enchantments.MULTISHOT) > 0) {
            helper.fail("Disabled toggles should leave the pillager crossbow unenchanted");
        }
        helper.succeed();
    }

    // ---- Witch / Ravager / Guardian: tier tags (also forces the mixins on
    //      these classes to apply, validating their injection points) ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void witch_abilityTagsByTier(GameTestHelper helper) {
        Mob witch = helper.spawnWithNoFreeWill(EntityTypes.WITCH, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(witch, 5, "witch", cfg);
        if (!witch.entityTags().contains(AbilityManager.TAG_LINGERING_POTIONS)) {
            helper.fail("Tier-5 witch should have lingering potions (tier-3 ability)");
        }
        if (!witch.entityTags().contains(AbilityManager.TAG_AGGRESSIVE_HEALING)) {
            helper.fail("Tier-5 witch should have aggressive healing");
        }

        Mob lowWitch = helper.spawnWithNoFreeWill(EntityTypes.WITCH, new BlockPos(2, 2, 2));
        AbilityManager.applyAbilities(lowWitch, 2, "witch", cfg);
        if (lowWitch.entityTags().contains(AbilityManager.TAG_LINGERING_POTIONS)
                || lowWitch.entityTags().contains(AbilityManager.TAG_AGGRESSIVE_HEALING)) {
            helper.fail("Tier-2 witch should have no tier-3/5 witch abilities");
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void ravager_roarTaggedAtTier3(GameTestHelper helper) {
        Mob ravager = helper.spawnWithNoFreeWill(EntityTypes.RAVAGER, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(ravager, 3, "ravager", cfg);
        if (!ravager.entityTags().contains(AbilityManager.TAG_RAVAGER_ROAR)) {
            helper.fail("Tier-3 ravager should have expanded roar");
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void guardian_beamTaggedAtTier3(GameTestHelper helper) {
        Mob guardian = helper.spawnWithNoFreeWill(EntityTypes.GUARDIAN, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(guardian, 3, "guardian", cfg);
        if (!guardian.entityTags().contains(AbilityManager.TAG_GUARDIAN_BEAM)) {
            helper.fail("Tier-3 guardian should have faster beam");
        }
        helper.succeed();
    }

    // ---- Vindicator: door breaking on any difficulty ----

    @GameTest(structure = "tribulation:empty_3x3")
    public void vindicator_breaksDoorsOnAnyDifficultyAtTier3(GameTestHelper helper) {
        Vindicator vindicator = (Vindicator) helper.spawnWithNoFreeWill(EntityTypes.VINDICATOR, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(vindicator, 3, "vindicator", cfg);

        // A goal whose own predicate rejects every difficulty: only the tier tag
        // (via BreakDoorGoalMixin) can make it valid on NORMAL.
        BreakDoorGoal goal = new BreakDoorGoal(vindicator, difficulty -> false);
        if (!isValidDifficulty(helper, goal, Difficulty.NORMAL)) {
            helper.fail("Tier-3 vindicator should break doors on NORMAL difficulty");
        }
        helper.succeed();
    }

    @GameTest(structure = "tribulation:empty_3x3")
    public void vindicator_doorBreakingGatedBelowTier3(GameTestHelper helper) {
        Vindicator vindicator = (Vindicator) helper.spawnWithNoFreeWill(EntityTypes.VINDICATOR, SPAWN);
        TribulationConfig cfg = new TribulationConfig();
        AbilityManager.applyAbilities(vindicator, 1, "vindicator", cfg);

        BreakDoorGoal goal = new BreakDoorGoal(vindicator, difficulty -> false);
        if (isValidDifficulty(helper, goal, Difficulty.NORMAL)) {
            helper.fail("Tier-1 vindicator should not break doors on NORMAL difficulty");
        }
        helper.succeed();
    }

    // ---- Helpers ----

    /**
     * Invoke the mob's protected {@code getArrow} and return the produced arrow.
     * Reflection is used because {@code getArrow} is protected; gametests run
     * with named mappings so the name resolves. The lookup walks up the class
     * hierarchy because a plain {@link Skeleton} inherits {@code getArrow} from
     * {@code AbstractSkeleton} instead of declaring its own.
     */
    private static AbstractArrow produceArrow(GameTestHelper helper, Mob shooter) {
        try {
            // Vanilla signature is getArrow(arrowStack, velocity, weaponStack).
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            ItemStack bow = new ItemStack(Items.BOW);
            for (Class<?> type = shooter.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Method getArrow = type.getDeclaredMethod(
                            "getArrow", ItemStack.class, float.class, ItemStack.class);
                    getArrow.setAccessible(true);
                    return (AbstractArrow) getArrow.invoke(shooter, arrowStack, 1.0f, bow);
                } catch (NoSuchMethodException ignored) {
                    // Keep walking up to the class that declares getArrow.
                }
            }
            throw helper.assertionException(Component.literal("No getArrow found on " + shooter.getClass()));
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw helper.assertionException(Component.literal("getArrow threw: " + e.getCause()));
        } catch (ReflectiveOperationException e) {
            throw helper.assertionException(Component.literal("Reflection on getArrow failed: " + e));
        }
    }

    /**
     * Return the highest amplifier of {@code effect} among the stored effects of
     * the arrow {@code shooter} produces, or {@code -1} if absent.
     */
    private static int maxArrowEffectAmplifier(GameTestHelper helper, Mob shooter,
                                               net.minecraft.world.effect.MobEffect effect) {
        AbstractArrow arrow = produceArrow(helper, shooter);
        try {
            Method getPotionContents = arrow.getClass().getDeclaredMethod("getPotionContents");
            getPotionContents.setAccessible(true);
            PotionContents contents = (PotionContents) getPotionContents.invoke(arrow);

            int max = -1;
            for (MobEffectInstance instance : contents.customEffects()) {
                if (instance.getEffect().value() == effect) {
                    max = Math.max(max, instance.getAmplifier());
                }
            }
            return max;
        } catch (java.lang.reflect.InvocationTargetException e) {
            helper.fail("getPotionContents threw: " + e.getCause());
            return -1;
        } catch (ReflectiveOperationException e) {
            helper.fail("Reflection on getPotionContents failed: " + e);
            return -1;
        }
    }

    private static int enchantLevel(GameTestHelper helper, Mob mob,
                                    net.minecraft.resources.ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, mob.getMainHandItem());
    }

    private static boolean isValidDifficulty(GameTestHelper helper, BreakDoorGoal goal, Difficulty difficulty) {
        try {
            Method method = BreakDoorGoal.class.getDeclaredMethod("isValidDifficulty", Difficulty.class);
            method.setAccessible(true);
            return (boolean) method.invoke(goal, difficulty);
        } catch (ReflectiveOperationException e) {
            helper.fail("Reflection on isValidDifficulty failed: " + e);
            return false;
        }
    }
}
