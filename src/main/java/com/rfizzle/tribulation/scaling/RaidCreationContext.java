package com.rfizzle.tribulation.scaling;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Carries the {@link ServerLevel} of a raid while vanilla code that may call
 * {@code Raid.getNumGroups} runs.
 *
 * <p>Since 26.1 a raid no longer holds its level. The first {@code getNumGroups} call is
 * where raid scaling captures the targeted players' tier: the live
 * {@code Raid(BlockPos, Difficulty)} constructor for a new raid, or {@code spawnGroup} for a
 * raid restored from disk. {@code RaidsMixin} publishes the level around the constructor and
 * {@code RaidScalingMixin} around {@code spawnGroup}, so the tier can still be resolved.
 */
public final class RaidCreationContext {

    private static final ThreadLocal<ServerLevel> LEVEL = new ThreadLocal<>();

    private RaidCreationContext() {
    }

    /** Runs {@code action} with {@code level} published, clearing it even if the action throws. */
    public static <T> T withLevel(ServerLevel level, Supplier<T> action) {
        ServerLevel previous = LEVEL.get();
        LEVEL.set(level);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                LEVEL.remove();
            } else {
                LEVEL.set(previous);
            }
        }
    }

    /** The level published on this thread, or {@code null} outside {@link #withLevel}. */
    public static @Nullable ServerLevel level() {
        return LEVEL.get();
    }
}
