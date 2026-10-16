package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public final class BountyFeature {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BeastsAndBountiesMod.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BeastsAndBountiesMod.MODID);

    public static final DeferredBlock<BountyBoardBlock> BOUNTY_BOARD = BLOCKS.registerBlock("bounty_board", BountyBoardBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f).sound(SoundType.WOOD).noCollission().noOcclusion());
    public static final DeferredItem<BlockItem> BOUNTY_BOARD_ITEM = ITEMS.registerSimpleBlockItem(BOUNTY_BOARD);

    private BountyFeature() {}

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, BountyConfig.SPEC, "beasts_and_bounties-bounty-server.toml");
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        PlayerBounties.register(modEventBus);
        BountySounds.register(modEventBus);
        modEventBus.addListener(BountyPayloads::register);
        modEventBus.addListener(BountyFeature::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(BOUNTY_BOARD_ITEM);
    }
}
