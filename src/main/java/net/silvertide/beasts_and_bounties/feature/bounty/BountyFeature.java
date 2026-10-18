package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public final class BountyFeature {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BeastsAndBountiesMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BeastsAndBountiesMod.MODID);

    public static final RegistryObject<BountyBoardBlock> BOUNTY_BOARD = BLOCKS.register("bounty_board", () -> new BountyBoardBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f).sound(SoundType.WOOD).noCollission().noOcclusion()));
    public static final RegistryObject<Item> BOUNTY_BOARD_ITEM = ITEMS.register("bounty_board", () -> new BlockItem(BOUNTY_BOARD.get(), new Item.Properties()));

    private BountyFeature() {}

    public static void init(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BountyConfig.SPEC, "beasts_and_bounties-bounty-server.toml");
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BountySounds.register(modEventBus);
        BountyPayloads.register();
        modEventBus.addListener(BountyFeature::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(BOUNTY_BOARD_ITEM);
    }
}
