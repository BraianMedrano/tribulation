package com.rfizzle.tribulation.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkeletonWeaponSwitchTest {

    private static double sq(double blocks) {
        return blocks * blocks;
    }

    @Test
    void drawsMeleeWhenTargetClosesInWhileHoldingBow() {
        assertEquals(SkeletonWeaponSwitch.Action.SWAP,
                SkeletonWeaponSwitch.decide(true, true, true, sq(3.0)));
    }

    @Test
    void drawsBowWhenTargetBacksOffWhileHoldingMelee() {
        assertEquals(SkeletonWeaponSwitch.Action.SWAP,
                SkeletonWeaponSwitch.decide(true, true, false, sq(10.0)));
    }

    @Test
    void keepsBowAtRange() {
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(true, true, true, sq(10.0)));
    }

    @Test
    void keepsMeleeUpClose() {
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(true, true, false, sq(2.0)));
    }

    @Test
    void hysteresisBandKeepsWhicheverWeaponIsDrawn() {
        double between = sq((SkeletonWeaponSwitch.MELEE_RANGE + SkeletonWeaponSwitch.BOW_RANGE) / 2.0);
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(true, true, true, between));
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(true, true, false, between));
    }

    @Test
    void thresholdsAreInclusive() {
        assertEquals(SkeletonWeaponSwitch.Action.SWAP,
                SkeletonWeaponSwitch.decide(true, true, true, sq(SkeletonWeaponSwitch.MELEE_RANGE)));
        assertEquals(SkeletonWeaponSwitch.Action.SWAP,
                SkeletonWeaponSwitch.decide(true, true, false, sq(SkeletonWeaponSwitch.BOW_RANGE)));
    }

    @Test
    void neverSwapsWithoutAStashedWeapon() {
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(true, false, true, sq(1.0)));
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(true, false, false, sq(20.0)));
    }

    @Test
    void neverSwapsWithoutATarget() {
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(false, true, true, sq(1.0)));
        assertEquals(SkeletonWeaponSwitch.Action.KEEP,
                SkeletonWeaponSwitch.decide(false, true, false, sq(20.0)));
    }
}
