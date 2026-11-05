package com.rfizzle.tribulation.ability;

import com.rfizzle.tribulation.data.TribulationAttachments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Swaps a {@code skeleton_sword_switch} skeleton between its bow and its melee
 * weapon as the target closes in or backs off (see {@link SkeletonWeaponSwitch}).
 *
 * <p>Inert unless the skeleton carries a {@link TribulationAttachments#STASHED_WEAPON},
 * which only the ability sets, so every other skeleton keeps vanilla AI.
 *
 * <p>This must run <em>after</em> the goal selector ticks, never from inside a
 * {@code Goal}: equipping through {@code setItemSlot} makes vanilla's
 * {@code reassessWeaponGoal} remove and re-add the bow and melee goals, and doing
 * that while {@code GoalSelector.tick} iterates its goal set corrupts the
 * iteration and crashes the server. {@code MobServerAiStepMixin} calls
 * {@link #tick} at the end of {@code Mob.serverAiStep}, so the swapped goal is
 * picked up on the next AI step.
 */
public final class SkeletonWeaponSwitcher {

    private SkeletonWeaponSwitcher() {}

    /**
     * Cheap enough to run every AI step: one attachment lookup (absent on every
     * skeleton without the ability) and, only when a weapon is stashed, one
     * distance check.
     */
    public static void tick(AbstractSkeleton skeleton) {
        ItemStack stashed = skeleton.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY);
        if (stashed.isEmpty()) return;
        LivingEntity target = skeleton.getTarget();
        boolean hasTarget = target != null && target.isAlive();
        SkeletonWeaponSwitch.Action action = SkeletonWeaponSwitch.decide(
                hasTarget,
                true,
                skeleton.getMainHandItem().is(Items.BOW),
                hasTarget ? skeleton.distanceToSqr(target) : 0.0);
        if (action != SkeletonWeaponSwitch.Action.SWAP) return;

        ItemStack drawn = skeleton.getMainHandItem();
        if (drawn.isEmpty()) {
            skeleton.removeAttached(TribulationAttachments.STASHED_WEAPON);
        } else {
            skeleton.setAttached(TribulationAttachments.STASHED_WEAPON, drawn);
        }
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, stashed);
    }
}
