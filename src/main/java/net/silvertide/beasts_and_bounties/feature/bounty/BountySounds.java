package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.function.Supplier;

public final class BountySounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, BeastsAndBountiesMod.MODID);

    public static final Supplier<SoundEvent> CLAIMED =
            SOUND_EVENTS.register("bounty.claimed", () -> SoundEvent.createVariableRangeEvent(BeastsAndBountiesMod.id("bounty.claimed")));

    private BountySounds() {}

    static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
