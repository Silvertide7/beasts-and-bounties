package net.silvertide.beasts_and_bounties;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryFeature;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyFeature;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingFeature;

@Mod(BeastsAndBountiesMod.MODID)
public class BeastsAndBountiesMod {
    public static final String MODID = "beasts_and_bounties";

    public BeastsAndBountiesMod(IEventBus modEventBus, ModContainer modContainer) {
        BestiaryFeature.init(modEventBus);
        TrackingFeature.init(modEventBus, modContainer);
        BountyFeature.init(modEventBus, modContainer);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
