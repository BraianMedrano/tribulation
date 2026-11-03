package com.rfizzle.tribulation.particle;

import com.rfizzle.tribulation.Tribulation;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Custom particle types for Tribulation's threat-telegraphing cues and the
 * champion aura. Each is a {@link SimpleParticleType} rendered client-side from
 * a {@code .glyph}-authored sprite. The threat cues carry no data beyond their
 * identity. {@link #CHAMPION_AURA} has no payload type either, but it reads its
 * spawn velocity as data: the anchor's entity id and the spark's start angle
 * (see {@code ChampionAuraParticle}).
 *
 * <p>Registered into {@link BuiltInRegistries#PARTICLE_TYPE} on both logical
 * sides during {@code onInitialize}; the client-side {@code ParticleProvider}
 * factories that draw them live in {@code TribulationClient}.
 */
public final class TribulationParticles {
    /** Generic high-tier cue: a slow-rising dark red smoke puff on tier-4+ scaled mobs. */
    public static final SimpleParticleType THREAT_TIER = FabricParticleTypes.simple();
    /** Big Zombie cue: a heavy, low, sooty dust puff suggesting mass. */
    public static final SimpleParticleType THREAT_BIG = FabricParticleTypes.simple();
    /** Speed Zombie cue: a sharp pale afterimage streak suggesting velocity. */
    public static final SimpleParticleType THREAT_SPEED = FabricParticleTypes.simple();
    /** Champion aura: a golden spark that orbits a champion and climbs from feet to head. */
    public static final SimpleParticleType CHAMPION_AURA = FabricParticleTypes.simple();

    private static boolean registered = false;

    private TribulationParticles() {}

    public static void register() {
        if (registered) return;
        registered = true;

        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Tribulation.id("threat_tier"), THREAT_TIER);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Tribulation.id("threat_big"), THREAT_BIG);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Tribulation.id("threat_speed"), THREAT_SPEED);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Tribulation.id("champion_aura"), CHAMPION_AURA);
    }
}
