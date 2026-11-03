package com.rfizzle.tribulation.particle;

/**
 * Pure, Minecraft-free motion and appearance math for the client-side threat
 * and champion particles: the fade curve, the smoke puff's growth, its block
 * light floor, and the champion spark's orbit. Kept out of the particle classes
 * so it is unit-testable without bootstrapping the client; the particles are a
 * thin shell that feeds age, lifetime and the anchor's size into these
 * functions (see the {@code mc-world-render} skill's "keep the math pure").
 */
public final class ThreatParticleMath {
    /**
     * Lowest block light the tier smoke renders with. The smoke still takes the
     * world light, so it dims at night, but never below this level, so it stays
     * readable in a dark cave or under a moonless sky.
     */
    public static final int SMOKE_BLOCK_LIGHT_FLOOR = 9;

    /** Angular speed of a champion spark around its anchor, in radians per tick. */
    public static final double ORBIT_RADIANS_PER_TICK = 0.22;

    /** Smallest orbit radius, sized for a zombie-width (0.6) mob. */
    private static final double MIN_ORBIT_RADIUS = 0.55;
    /** Gap between the anchor's hitbox edge and the orbit, in blocks. */
    private static final double ORBIT_CLEARANCE = 0.25;
    /** Height above the feet where a spark starts its climb. */
    private static final double CLIMB_START = 0.15;
    /** Fraction of the anchor's height where a spark ends its climb. */
    private static final double CLIMB_END_FRACTION = 0.95;
    /** Ticks a spark takes to fade in, and to fade out at the end. */
    private static final float ORBIT_FADE_IN_TICKS = 4f;
    private static final float ORBIT_FADE_OUT_TICKS = 6f;

    private ThreatParticleMath() {}

    /** Quadratic ease-out over the lifetime, so a cue fades instead of popping. */
    public static float fadeAlpha(float baseAlpha, int age, int lifetime) {
        float f = lifetime <= 0 ? 1.0f : (float) age / (float) lifetime;
        return baseAlpha * (1.0f - f * f);
    }

    /**
     * Half-size of a quad that swells linearly from {@code base} to
     * {@code base + grow} over its lifetime, interpolated by the partial tick.
     */
    public static float grownSize(float base, float grow, int age, int lifetime, float partialTick) {
        return base + grow * progress(age + partialTick, lifetime);
    }

    /** The block light a floored particle renders with. */
    public static int flooredBlockLight(int blockLight, int floor) {
        return Math.max(blockLight, floor);
    }

    /** Orbit radius that keeps a spark just outside an anchor of this width. */
    public static double orbitRadius(double bbWidth) {
        return Math.max(MIN_ORBIT_RADIUS, bbWidth * 0.5 + ORBIT_CLEARANCE);
    }

    /** Height above the anchor's feet: climbs from the feet to the head over the life. */
    public static double climbHeight(double bbHeight, int age, int lifetime) {
        double end = bbHeight * CLIMB_END_FRACTION;
        return CLIMB_START + (end - CLIMB_START) * progress(age, lifetime);
    }

    /** Alpha envelope of a spark: a short fade in, full brightness, a short fade out. */
    public static float orbitAlpha(int age, int lifetime) {
        float in = age / ORBIT_FADE_IN_TICKS;
        float out = (lifetime - age) / ORBIT_FADE_OUT_TICKS;
        return Math.clamp(Math.min(in, out), 0f, 1f);
    }

    private static float progress(float age, int lifetime) {
        if (lifetime <= 0) return 1.0f;
        return Math.clamp(age / lifetime, 0f, 1f);
    }
}
