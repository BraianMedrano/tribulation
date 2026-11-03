package com.rfizzle.tribulation.event;

import com.rfizzle.tribulation.ability.AbilityManager;
import com.rfizzle.tribulation.champion.ChampionManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;

/**
 * Brings mobs saved by older builds up to current behavior when they load.
 *
 * <p>Scaling runs once per mob: {@link MobScalingHandler#PROCESSED_TAG} skips
 * it on every later load, so a mob saved before a behavior change keeps the
 * state the old build gave it. Each step here matches only the exact state the
 * old build left, so it is a no-op on current mobs and runs at most once per
 * mob in effect: the fix removes the state it matches.
 *
 * <p>Runs on every processed-mob load, so each check is a cheap field read.
 */
final class LegacyMobMigration {

    private LegacyMobMigration() {}

    static void migrate(Mob mob) {
        migrateFlameSkeleton(mob);
        migrateChampionName(mob);
    }

    /**
     * The old tier-4 flame-arrows ability set the skeleton alight for good and
     * gave it infinite Fire Resistance, expecting the arrows to inherit the
     * flames. They never did. Strip that state and tag the skeleton so its
     * arrows light as the ability now intends. An infinite Fire Resistance on a
     * plain skeleton comes only from that ability.
     */
    private static void migrateFlameSkeleton(Mob mob) {
        if (mob.getType() != EntityTypes.SKELETON) return;
        MobEffectInstance fireResistance = mob.getEffect(MobEffects.FIRE_RESISTANCE);
        if (fireResistance == null || !fireResistance.isInfiniteDuration()) return;

        mob.removeEffect(MobEffects.FIRE_RESISTANCE);
        mob.clearFire();
        mob.addTag(AbilityManager.TAG_FLAME_ARROWS);
    }

    /**
     * Older builds drew a champion's name always, and vanilla draws an
     * always-visible name through blocks, giving the champion away behind
     * walls. Champions now show it only while the crosshair is on them.
     */
    private static void migrateChampionName(Mob mob) {
        if (!mob.isCustomNameVisible()) return;
        if (!ChampionManager.isChampion(mob)) return;
        mob.setCustomNameVisible(false);
    }
}
