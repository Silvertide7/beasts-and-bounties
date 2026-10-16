package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.function.Supplier;

public final class TrackingParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, BeastsAndBountiesMod.MODID);

    public static final Supplier<SimpleParticleType> TRACKING_WISP = PARTICLE_TYPES.register("tracking_wisp", () -> new SimpleParticleType(false));

    private TrackingParticles() {}

    static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
