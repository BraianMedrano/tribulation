package com.rfizzle.meridian.api;

/**
 * Test stand-in for Meridian's {@code EnchantmentInfo}, mirroring only the members Tribulation
 * reaches by reflection: the {@code enabled()} record accessor and {@code getMaxLevel()}. Kept in
 * step with meridian-1.0.0 (both are public instance methods on a public final record).
 */
public record EnchantmentInfo(int maxLevel, boolean enabled) {

    public int getMaxLevel() {
        return maxLevel;
    }
}
