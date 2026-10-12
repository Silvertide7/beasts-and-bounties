package net.silvertide.beasts_and_bounties;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryFeature;

@Mod(BeastsAndBountiesMod.MODID)
public class BeastsAndBountiesMod {
    public static final String MODID = "beasts_and_bounties";

    public BeastsAndBountiesMod(IEventBus modEventBus) {
        BestiaryFeature.init(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
