package net.silvertide.beasts_and_bounties.feature.bestiary;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

@EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class DevFastMilestonesPack {
    private DevFastMilestonesPack() {}

    @SubscribeEvent
    static void addDevFastMilestonesPack(AddPackFindersEvent event) {
        event.addPackFinders(BeastsAndBountiesMod.id("builtin_data_packs/dev_fast_milestones"), PackType.SERVER_DATA,
                Component.literal("Beasts & Bounties Dev Fast Milestones"), PackSource.DEFAULT, false, Pack.Position.TOP);
    }
}
