package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public final class BountySounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BeastsAndBountiesMod.MODID);

    public static final RegistryObject<SoundEvent> CLAIMED =
            SOUND_EVENTS.register("bounty.claimed", () -> SoundEvent.createVariableRangeEvent(BeastsAndBountiesMod.id("bounty.claimed")));

    private BountySounds() {}

    static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
