package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.function.Supplier;

public final class TrackingSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, BeastsAndBountiesMod.MODID);

    public static final Supplier<SoundEvent> HUNT_START =
            SOUND_EVENTS.register("tracking.hunt_start", () -> SoundEvent.createVariableRangeEvent(BeastsAndBountiesMod.id("tracking.hunt_start")));

    private TrackingSounds() {}

    static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
