package com.rfizzle.tribulation.scaling;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Carries the {@link ServerLevel} of a raid that is being constructed.
 *
 * <p>Since 26.1 the live {@code Raid(BlockPos, Difficulty)} constructor no longer receives
 * the level, but it is where {@code Raid.getNumGroups} first runs, and that first call is
 * where raid scaling captures the targeted players' tier. {@code RaidsMixin} publishes the
 * level around the constructor call so {@code RaidScalingMixin} can still read it.
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

    /** The level of the raid under construction on this thread, or {@code null} outside one. */
    public static @Nullable ServerLevel level() {
        return LEVEL.get();
    }
}
