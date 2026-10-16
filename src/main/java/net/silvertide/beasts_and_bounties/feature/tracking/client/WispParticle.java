package net.silvertide.beasts_and_bounties.feature.tracking.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

final class WispParticle extends SimpleAnimatedParticle {
    private static final float GRAVITY = 0.0125f;
    private static final float SIZE_SCALE = 0.5f;
    private static final float BRIGHTNESS = 0.8f;
    private static final float MAX_ALPHA = 0.55f;
    private static final int BASE_LIFETIME_TICKS = 60;
    private static final int LIFETIME_VARIATION_TICKS = 12;

    private WispParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites, GRAVITY);
        quadSize *= SIZE_SCALE;
        lifetime = BASE_LIFETIME_TICKS + random.nextInt(LIFETIME_VARIATION_TICKS);
        setColor(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS);
        setAlpha(MAX_ALPHA);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float halfLife = lifetime / 2f;
        float fadeRemaining = age < halfLife ? 1f : Math.max(0f, 1f - (age - halfLife) / halfLife);
        alpha = MAX_ALPHA * fadeRemaining;
    }

    @Override
    public void move(double x, double y, double z) {
        setBoundingBox(getBoundingBox().move(x, y, z));
        setLocationFromBoundingbox();
    }

    static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new WispParticle(level, x, y, z, sprites);
        }
    }
}
