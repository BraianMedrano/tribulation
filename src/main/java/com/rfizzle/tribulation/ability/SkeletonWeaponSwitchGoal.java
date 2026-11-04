package com.rfizzle.tribulation.ability;

import com.rfizzle.tribulation.data.TribulationAttachments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Swaps a {@code skeleton_sword_switch} skeleton between its bow and its melee
 * weapon as the target closes in or backs off (see {@link SkeletonWeaponSwitch}).
 *
 * <p>Inert unless the skeleton carries a {@link TribulationAttachments#STASHED_WEAPON},
 * which only the ability sets, so every other skeleton keeps vanilla AI. The goal
 * declares no flags: it never moves or looks, so it runs alongside vanilla's attack
 * goals. Equipping through {@code setItemSlot} lets vanilla's
 * {@code reassessWeaponGoal} switch between the bow goal and the melee goal.
 */
public final class SkeletonWeaponSwitchGoal extends Goal {

    private final AbstractSkeleton skeleton;

    public SkeletonWeaponSwitchGoal(AbstractSkeleton skeleton) {
        this.skeleton = skeleton;
    }

    /**
     * Cheap enough to run on the selector's own cadence: one attachment lookup
     * (absent on every skeleton without the ability) and one distance check.
     */
    @Override
    public boolean canUse() {
        return decide() == SkeletonWeaponSwitch.Action.SWAP;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        if (decide() != SkeletonWeaponSwitch.Action.SWAP) return;
        ItemStack stashed = skeleton.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY);
        ItemStack drawn = skeleton.getMainHandItem();
        if (drawn.isEmpty()) {
            skeleton.removeAttached(TribulationAttachments.STASHED_WEAPON);
        } else {
            skeleton.setAttached(TribulationAttachments.STASHED_WEAPON, drawn);
        }
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, stashed);
    }

    private SkeletonWeaponSwitch.Action decide() {
        ItemStack stashed = skeleton.getAttachedOrElse(TribulationAttachments.STASHED_WEAPON, ItemStack.EMPTY);
        LivingEntity target = skeleton.getTarget();
        boolean hasTarget = target != null && target.isAlive();
        return SkeletonWeaponSwitch.decide(
                hasTarget,
                !stashed.isEmpty(),
                skeleton.getMainHandItem().is(Items.BOW),
                hasTarget ? skeleton.distanceToSqr(target) : 0.0);
    }
}
