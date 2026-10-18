package net.silvertide.beasts_and_bounties;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryFeature;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyFeature;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingFeature;

@Mod(BeastsAndBountiesMod.MODID)
public class BeastsAndBountiesMod {
    public static final String MODID = "beasts_and_bounties";

    public BeastsAndBountiesMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BestiaryFeature.init(modEventBus);
        TrackingFeature.init(modEventBus);
        BountyFeature.init(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
