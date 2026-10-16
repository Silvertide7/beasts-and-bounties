package net.silvertide.beasts_and_bounties.feature.tracking;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public final class TrackingFeature {
    private TrackingFeature() {}

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, TrackingConfig.SPEC, "beasts_and_bounties-tracking-server.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, TrackingClientConfig.SPEC, "beasts_and_bounties-tracking-client.toml");
        PlayerTracking.register(modEventBus);
        TrackingParticles.register(modEventBus);
        TrackingSounds.register(modEventBus);
        modEventBus.addListener(TrackingPayloads::register);
    }
}
