package net.silvertide.beasts_and_bounties.gametest.feature.bestiary;

import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryFeature;

@Mod.EventBusSubscriber(modid = BeastsAndBountiesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DevFastMilestonesPack {
    private DevFastMilestonesPack() {}

    @SubscribeEvent
    public static void addDevFastMilestonesPack(AddPackFindersEvent event) {
        BestiaryFeature.addBuiltInDataPack(event, "dev_fast_milestones", "builtin_data_packs/dev_fast_milestones", "Beasts & Bounties Dev Fast Milestones");
    }
}
