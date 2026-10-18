package net.silvertide.beasts_and_bounties.feature.bestiary;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.nio.file.Path;

public final class BestiaryFeature {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BeastsAndBountiesMod.MODID);

    public static final RegistryObject<Item> BESTIARY = ITEMS.register("bestiary", () -> new BestiaryItem(new Item.Properties().stacksTo(1)));

    private BestiaryFeature() {}

    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        BestiaryPayloads.register();
        modEventBus.addListener(BestiaryFeature::addToCreativeTab);
        modEventBus.addListener(BestiaryFeature::addVanillaEntriesPack);
    }

    public static void addBuiltInDataPack(AddPackFindersEvent event, String id, String folder, String title) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        Path path = ModList.get().getModFileById(BeastsAndBountiesMod.MODID).getFile().findResource(folder);
        Pack pack = Pack.readMetaAndCreate(BeastsAndBountiesMod.id(id).toString(), Component.literal(title), false,
                name -> new PathPackResources(name, path, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.DEFAULT);
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    private static void addVanillaEntriesPack(AddPackFindersEvent event) {
        addBuiltInDataPack(event, "builtin_data_packs/vanilla_bestiary", "builtin_data_packs/vanilla_bestiary", "Beasts & Bounties Vanilla Bestiary");
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(BESTIARY);
    }
}
