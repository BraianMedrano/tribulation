package com.rfizzle.tribulation.client;

import com.rfizzle.tribulation.particle.ThreatParticleMath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/**
 * Client-side threat-telegraphing particle. A single class drives all three
 * cues; the {@link Style} bakes in the per-cue motion (rise/sink/drift),
 * lifetime, size, base alpha, growth, sprite selection and lighting so each
 * reads distinctly. Alpha eases out over the cue's lifetime so it fades rather
 * than popping. Purely cosmetic — no physics, no collision.
 */
public class ThreatParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final Style style;
    private final float baseAlpha;

    protected ThreatParticle(ClientLevel level, double x, double y, double z,
                             double xd, double yd, double zd, SpriteSet sprites, Style style) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.style = style;
        this.friction = style.friction;
        this.hasPhysics = false;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = style.gravity;
        this.lifetime = style.minLife + this.random.nextInt(style.lifeJitter + 1);
        this.quadSize = style.size * (0.85f + this.random.nextFloat() * 0.3f);
        this.baseAlpha = style.alpha;
        this.alpha = style.alpha;
        if (style.spriteByAge) {
            this.setSpriteFromAge(sprites);
        } else {
            this.setSprite(sprites.get(this.random));
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = ThreatParticleMath.fadeAlpha(this.baseAlpha, this.age, this.lifetime);
        if (style.spriteByAge) {
            this.setSpriteFromAge(sprites);
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        return ThreatParticleMath.grownSize(this.quadSize, style.grow, this.age, this.lifetime, partialTick);
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return style.lightCoords(super.getLightCoords(partialTick));
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    /** Per-cue motion and appearance presets. */
    public enum Style {
        /**
         * Dark red smoke: a puff that rises slowly, swells and cools from rust to
         * soot through its six age frames. Never darker than the smoke light floor.
         */
        TIER(-0.008f, 28, 16, 0.10f, 0.80f, 0.96f, 0.24f, true, ThreatParticleMath.SMOKE_BLOCK_LIGHT_FLOOR),
        /** Sooty dust: heavier, sinks, short-lived, larger. */
        BIG(0.012f, 16, 12, 0.16f, 0.90f, 0.94f, 0f, false, ThreatParticleMath.NO_LIGHT_FLOOR),
        /** Afterimage streak: no gravity, quick, sharp. */
        SPEED(0.0f, 8, 8, 0.12f, 0.80f, 0.94f, 0f, false, ThreatParticleMath.NO_LIGHT_FLOOR);

        final float gravity;
        final int minLife;
        final int lifeJitter;
        final float size;
        final float alpha;
        final float friction;
        /** Half-size added linearly over the lifetime; 0 keeps the size fixed. */
        final float grow;
        /** Pick the sprite from the particle's age instead of at random. */
        final boolean spriteByAge;
        /** Minimum block light to render with, or {@link ThreatParticleMath#NO_LIGHT_FLOOR}. */
        final int blockLightFloor;

        Style(float gravity, int minLife, int lifeJitter, float size, float alpha,
              float friction, float grow, boolean spriteByAge, int blockLightFloor) {
            this.gravity = gravity;
            this.minLife = minLife;
            this.lifeJitter = lifeJitter;
            this.size = size;
            this.alpha = alpha;
            this.friction = friction;
            this.grow = grow;
            this.spriteByAge = spriteByAge;
            this.blockLightFloor = blockLightFloor;
        }

        /** The world's packed light with this cue's block light floor applied; sky light is kept. */
        int lightCoords(int coords) {
            int block = ThreatParticleMath.flooredBlockLight(LightCoordsUtil.block(coords), blockLightFloor);
            return LightCoordsUtil.withBlock(coords, block);
        }
    }

    /** Factory bound to one {@link Style}; registered once per particle type. */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Style style;

        public Provider(SpriteSet sprites, Style style) {
            this.sprites = sprites;
            this.style = style;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return new ThreatParticle(level, x, y, z, xd, yd, zd, sprites, style);
        }
    }
}
