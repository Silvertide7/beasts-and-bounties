package net.silvertide.beasts_and_bounties.feature.bestiary;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public final class BestiaryFeature {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BeastsAndBountiesMod.MODID);

    public static final DeferredItem<Item> BESTIARY = ITEMS.registerItem("bestiary", BestiaryItem::new, new Item.Properties().stacksTo(1));

    private BestiaryFeature() {}

    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        BestiaryProgress.register(modEventBus);
        modEventBus.addListener(BestiaryPayloads::register);
        modEventBus.addListener(BestiaryFeature::addToCreativeTab);
        modEventBus.addListener(BestiaryFeature::addVanillaEntriesPack);
    }

    private static void addVanillaEntriesPack(AddPackFindersEvent event) {
        event.addPackFinders(BeastsAndBountiesMod.id("builtin_data_packs/vanilla_bestiary"), PackType.SERVER_DATA,
                Component.literal("Beasts & Bounties Vanilla Bestiary"), PackSource.DEFAULT, false, Pack.Position.TOP);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(BESTIARY);
    }
}
