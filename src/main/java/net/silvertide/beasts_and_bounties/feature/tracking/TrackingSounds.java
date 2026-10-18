package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public final class TrackingSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BeastsAndBountiesMod.MODID);

    public static final RegistryObject<SoundEvent> HUNT_START =
            SOUND_EVENTS.register("tracking.hunt_start", () -> SoundEvent.createVariableRangeEvent(BeastsAndBountiesMod.id("tracking.hunt_start")));

    private TrackingSounds() {}

    static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
