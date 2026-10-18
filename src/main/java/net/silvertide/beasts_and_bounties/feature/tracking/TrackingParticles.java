package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public final class TrackingParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BeastsAndBountiesMod.MODID);

    public static final RegistryObject<SimpleParticleType> TRACKING_WISP = PARTICLE_TYPES.register("tracking_wisp", () -> new SimpleParticleType(false));

    private TrackingParticles() {}

    static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
