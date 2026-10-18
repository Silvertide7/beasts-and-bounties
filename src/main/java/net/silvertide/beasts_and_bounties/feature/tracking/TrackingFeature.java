package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class TrackingFeature {
    private TrackingFeature() {}

    public static void init(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TrackingConfig.SPEC, "beasts_and_bounties-tracking-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TrackingClientConfig.SPEC, "beasts_and_bounties-tracking-client.toml");
        TrackingParticles.register(modEventBus);
        TrackingSounds.register(modEventBus);
        TrackingPayloads.register();
    }
}
