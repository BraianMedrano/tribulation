package com.rfizzle.tribulation.client;

import com.rfizzle.tribulation.particle.ThreatParticleMath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * A golden spark that circles a champion and climbs from its feet to its head.
 * It follows its anchor every tick, so the orbit stays on the mob while it
 * walks, and it removes itself as soon as the anchor is gone. Full-bright, so a
 * champion reads as special in any light. Purely cosmetic.
 *
 * <p>The anchor travels through the particle's spawn arguments: the emitter
 * passes the entity's network id as {@code xd} and the start angle as
 * {@code yd}, which keeps the type a plain {@link SimpleParticleType}.
 */
public class ChampionAuraParticle extends SingleQuadParticle {
    private static final int LIFETIME_TICKS = 44;
    private static final float HALF_SIZE = 0.07f;
    private static final int FLICKER_TICKS = 3;

    private final Entity anchor;
    private final SpriteSet sprites;
    private double angle;

    protected ChampionAuraParticle(ClientLevel level, Entity anchor, double startAngle, SpriteSet sprites) {
        super(level, anchor.getX(), anchor.getY(), anchor.getZ(), sprites.first());
        this.anchor = anchor;
        this.sprites = sprites;
        this.angle = startAngle;
        this.lifetime = LIFETIME_TICKS;
        this.quadSize = HALF_SIZE;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.alpha = 0f;
        this.setSprite(sprites.get(this.random));
        place();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime || anchor.isRemoved()) {
            this.remove();
            return;
        }
        angle += ThreatParticleMath.ORBIT_RADIANS_PER_TICK;
        place();
        this.alpha = ThreatParticleMath.orbitAlpha(this.age, this.lifetime);
        if (this.age % FLICKER_TICKS == 0) {
            this.setSprite(sprites.get(this.random));
        }
    }

    private void place() {
        double radius = ThreatParticleMath.orbitRadius(anchor.getBbWidth());
        this.setPos(
                anchor.getX() + Math.cos(angle) * radius,
                anchor.getY() + ThreatParticleMath.climbHeight(anchor.getBbHeight(), this.age, this.lifetime),
                anchor.getZ() + Math.sin(angle) * radius);
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    /** Resolves the anchor from the spawn arguments; spawns nothing if it is gone. */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level,
                                                 double x, double y, double z,
                                                 double anchorId, double startAngle, double unused,
                                                 RandomSource random) {
            Entity anchor = level.getEntity((int) anchorId);
            if (anchor == null || anchor.isRemoved()) return null;
            return new ChampionAuraParticle(level, anchor, startAngle, sprites);
        }
    }
}
