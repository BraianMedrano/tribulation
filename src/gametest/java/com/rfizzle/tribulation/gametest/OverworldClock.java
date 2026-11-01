package com.rfizzle.tribulation.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;

/**
 * Reads and sets the overworld day clock for gametests. 26.x replaced
 * {@code Level.getDayTime()} / {@code ServerLevel.setDayTime(long)} with world
 * clocks; these helpers keep the tests' save-set-restore pattern readable.
 */
final class OverworldClock {
    private OverworldClock() {}

    static long get(ServerLevel level) {
        return level.getOverworldClockTime();
    }

    static void set(ServerLevel level, long ticks) {
        level.getServer().clockManager()
                .setTotalTicks(level.registryAccess().getOrThrow(WorldClocks.OVERWORLD), ticks);
    }
}
