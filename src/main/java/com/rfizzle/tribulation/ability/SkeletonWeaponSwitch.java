package com.rfizzle.tribulation.ability;

/**
 * Pure decision core for the {@code skeleton_sword_switch} ability: a skeleton
 * carrying a bow and a melee weapon draws the melee weapon when its target
 * closes in and goes back to the bow once the target backs off.
 *
 * <p>The two thresholds form a hysteresis band: between {@link #MELEE_RANGE}
 * and {@link #BOW_RANGE} the skeleton keeps whichever weapon is already drawn,
 * so a target standing on the edge does not make it swap every check.
 */
public final class SkeletonWeaponSwitch {

    /** Target distance (blocks) at or under which the bow is put away for melee. */
    public static final double MELEE_RANGE = 4.0;
    /** Target distance (blocks) at or over which the melee weapon is put away for the bow. */
    public static final double BOW_RANGE = 7.0;

    private static final double MELEE_RANGE_SQR = MELEE_RANGE * MELEE_RANGE;
    private static final double BOW_RANGE_SQR = BOW_RANGE * BOW_RANGE;

    public enum Action { KEEP, SWAP }

    private SkeletonWeaponSwitch() {}

    /**
     * @param hasTarget      whether the skeleton currently has an attack target
     * @param hasStash       whether a second weapon is stashed to swap with
     * @param holdingBow     whether the drawn (main-hand) weapon is the bow
     * @param targetDistSqr  squared distance to the target, in blocks
     */
    public static Action decide(boolean hasTarget, boolean hasStash, boolean holdingBow, double targetDistSqr) {
        if (!hasTarget || !hasStash) return Action.KEEP;
        if (holdingBow) {
            return targetDistSqr <= MELEE_RANGE_SQR ? Action.SWAP : Action.KEEP;
        }
        return targetDistSqr >= BOW_RANGE_SQR ? Action.SWAP : Action.KEEP;
    }
}
