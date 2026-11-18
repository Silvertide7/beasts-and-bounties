package net.silvertide.beasts_and_bounties.gametest.feature.bounty;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.Commands;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.util.GsonHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.gametest.MockPlayers;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyBoardBlock;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyBoardData;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyConfig;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyFeature;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyDefinition;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyDefinitions;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyOffers;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyReward;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyState;
import net.silvertide.beasts_and_bounties.feature.bounty.PlayerBounties;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.Classification;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;
import net.silvertide.beasts_and_bounties.feature.bestiary.Progress;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Optional;

@GameTestHolder(BeastsAndBountiesMod.MODID)
@PrefixGameTestTemplate(false)
public class BountyGameTests {
    private static final ResourceLocation ZOMBIE = EntityType.getKey(EntityType.ZOMBIE);
    private static final int SHIPPED_BOUNTIES = 36;

    @GameTest(template = "empty")
    public static void everyShippedBountyMatchesAnEntry(GameTestHelper helper) {
        helper.assertTrue(BountyDefinitions.all().size() == SHIPPED_BOUNTIES, "expected " + SHIPPED_BOUNTIES + " bounties, loaded " + BountyDefinitions.all().size());
        BountyDefinitions.all().keySet().forEach(entityId -> helper.assertTrue(BestiaryEntries.get(entityId) != null, entityId + " has a bounty but no entry"));
        BountyDefinition zombie = BountyDefinitions.all().get(ZOMBIE);
        helper.assertTrue(zombie.kills() == 8 && zombie.bonusKills() == 10 && zombie.experienceReward() == 100 && zombie.itemRewards().get(0).is(Items.IRON_INGOT) && zombie.itemRewards().get(0).getCount() == 12, "zombie bounty values");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skillExperienceRewardsParse(GameTestHelper helper) {
        JsonObject json = GsonHelper.parse("""
                {"rewards": [{"type": "pmmo_xp", "xp": "combat/50"}, {"type": "experience", "amount": 10}]}
                """);
        BountyDefinition definition = BountyDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, error -> {});
        List<BountyReward.SkillExperience> skills = definition.skillExperienceRewards();
        helper.assertTrue(skills.size() == 1 && skills.get(0).skill().equals("combat") && skills.get(0).amount() == 50, "pmmo_xp parses skill/amount");
        helper.assertTrue(definition.experienceReward() == 10 && definition.itemRewards().isEmpty(), "other rewards unaffected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void balancedBoardsHoldAnEliteOrBossAndANormal(GameTestHelper helper) {
        for (long period = 1; period <= 20; period++) {
            List<BountyOffers.Offer> offers = BountyOffers.pick(42, period, 0, 3, true, BountyDefinitions.all(), Set.of());
            helper.assertTrue(offers.size() == 3 && offers.stream().map(BountyOffers.Offer::entityId).distinct().count() == 3, "three distinct offers");
            helper.assertTrue(offers.stream().anyMatch(offer -> BestiaryEntries.get(offer.entityId()).classification() != Classification.NORMAL), "period " + period + " holds an elite or boss");
            helper.assertTrue(offers.stream().anyMatch(offer -> BestiaryEntries.get(offer.entityId()).classification() == Classification.NORMAL), "period " + period + " holds a normal creature");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void perBoardSeedsDifferByPlaceAndHoldInPlace(GameTestHelper helper) {
        GlobalPos board = GlobalPos.of(Level.OVERWORLD, new BlockPos(100, 64, -40));
        GlobalPos besideIt = GlobalPos.of(Level.OVERWORLD, new BlockPos(101, 64, -40));
        GlobalPos sameSpotInTheNether = GlobalPos.of(Level.NETHER, board.pos());
        helper.assertTrue(BountyOffers.boardSeed(42, board) == BountyOffers.boardSeed(42, GlobalPos.of(Level.OVERWORLD, new BlockPos(100, 64, -40))), "a board in the same spot keeps its seed");
        List<BountyOffers.Offer> boardOffers = BountyOffers.pick(BountyOffers.boardSeed(42, board), 1, 0, 3, false, BountyDefinitions.all(), Set.of());
        helper.assertTrue(!creatures(boardOffers).equals(creatures(BountyOffers.pick(BountyOffers.boardSeed(42, besideIt), 1, 0, 3, false, BountyDefinitions.all(), Set.of()))), "a board one block over rolls different postings");
        helper.assertTrue(!creatures(boardOffers).equals(creatures(BountyOffers.pick(BountyOffers.boardSeed(42, sameSpotInTheNether), 1, 0, 3, false, BountyDefinitions.all(), Set.of()))), "the same spot in another dimension rolls different postings");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void perBoardAcceptsOnlyAtAReachableBoard(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        BlockPos absoluteBoard = standAtABoard(helper, player);
        boolean perBoardPostingsBefore = BountyConfig.PER_BOARD_POSTINGS.get();
        BountyConfig.PER_BOARD_POSTINGS.set(true);
        try {
            ResourceLocation offered = BountyOffers.forBoard(player.server, GlobalPos.of(player.level().dimension(), absoluteBoard)).get(0).entityId();
            PlayerBounties.accept(player, absoluteBoard.offset(0, 0, 40), offered);
            helper.assertTrue(PlayerBounties.state(player).active().isEmpty(), "an accept from a spot with no reachable board is refused");
            PlayerBounties.accept(player, absoluteBoard, offered);
            helper.assertTrue(PlayerBounties.state(player).active().isPresent(), "an accept at a reachable board takes its posting");
        } finally {
            BountyConfig.PER_BOARD_POSTINGS.set(perBoardPostingsBefore);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boardHangsOnAWallAndPopsOffWithoutSupport(GameTestHelper helper) {
        BlockPos wallPos = new BlockPos(2, 2, 0);
        BlockPos wallBoardPos = new BlockPos(1, 2, 0);
        BlockPos floorPos = new BlockPos(0, 1, 4);
        BlockPos standingBoardPos = new BlockPos(0, 2, 4);
        helper.setBlock(wallPos, Blocks.STONE);
        helper.setBlock(floorPos, Blocks.STONE);

        BlockPos absoluteWallPos = helper.absolutePos(wallPos);
        BlockHitResult westFaceOfWall = new BlockHitResult(Vec3.atCenterOf(absoluteWallPos), Direction.WEST, absoluteWallPos, false);
        BlockPlaceContext placeOnWall = new BlockPlaceContext(MockPlayers.inLevel(helper), InteractionHand.MAIN_HAND, new ItemStack(BountyFeature.BOUNTY_BOARD_ITEM.get()), westFaceOfWall);
        BlockState wallBoard = BountyFeature.BOUNTY_BOARD.get().getStateForPlacement(placeOnWall);
        helper.assertTrue(wallBoard.getValue(BountyBoardBlock.WALL) && wallBoard.getValue(BountyBoardBlock.FACING) == Direction.WEST, "clicking a wall's side hangs the board facing away from it");

        helper.setBlock(wallBoardPos, wallBoard);
        helper.setBlock(standingBoardPos, BountyFeature.BOUNTY_BOARD.get().defaultBlockState());
        helper.setBlock(wallPos, Blocks.AIR);
        helper.setBlock(floorPos, Blocks.AIR);

        helper.assertBlockNotPresent(BountyFeature.BOUNTY_BOARD.get(), wallBoardPos);
        helper.assertBlockNotPresent(BountyFeature.BOUNTY_BOARD.get(), standingBoardPos);
        helper.assertItemEntityPresent(BountyFeature.BOUNTY_BOARD_ITEM.get(), wallBoardPos, 1);
        helper.assertItemEntityPresent(BountyFeature.BOUNTY_BOARD_ITEM.get(), standingBoardPos, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void offersAreStableWithinAPeriodAndChangeAcrossPeriods(GameTestHelper helper) {
        List<BountyOffers.Offer> first = BountyOffers.pick(42, 1, 0, 3, false, BountyDefinitions.all(), Set.of());
        List<BountyOffers.Offer> again = BountyOffers.pick(42, 1, 0, 3, false, BountyDefinitions.all(), Set.of());
        helper.assertTrue(first.size() == 3, "three offers are shown");
        helper.assertTrue(first.stream().map(BountyOffers.Offer::entityId).distinct().count() == 3, "offers are distinct");
        helper.assertTrue(first.equals(again), "the same period gives the same offers");
        boolean anyDifferent = false;
        for (long period = 2; period <= 6; period++) anyDifferent |= !BountyOffers.pick(42, period, 0, 3, false, BountyDefinitions.all(), Set.of()).equals(first);
        helper.assertTrue(anyDifferent, "later periods give different offers");
        boolean anyRerollDiffers = false;
        for (int rerolls = 1; rerolls <= 5; rerolls++) anyRerollDiffers |= !BountyOffers.pick(42, 1, rerolls, 3, false, BountyDefinitions.all(), Set.of()).equals(first);
        helper.assertTrue(anyRerollDiffers, "a reroll gives different offers in the same period");
        Set<ResourceLocation> firstIds = first.stream().map(BountyOffers.Offer::entityId).collect(Collectors.toSet());
        List<BountyOffers.Offer> next = BountyOffers.pick(42, 2, 0, 3, false, BountyDefinitions.all(), firstIds);
        helper.assertTrue(next.stream().noneMatch(offer -> firstIds.contains(offer.entityId())), "the next board never repeats the previous one");
        ServerPlayer one = MockPlayers.inLevel(helper);
        ServerPlayer two = MockPlayers.inLevel(helper);
        helper.assertTrue(BountyOffers.current(one.server).equals(BountyOffers.current(two.server)), "the board is shared by every player");
        ServerLevel overworld = one.server.overworld();
        long savedDayTime = overworld.getDayTime();
        overworld.setDayTime(Level.TICKS_PER_DAY * 10 + 100);
        List<BountyOffers.Offer> today = BountyOffers.current(one.server);
        helper.assertTrue(BountyOffers.dawnsUntilRefresh(one.server) == 1, "with the default config the board refreshes at the next dawn");
        overworld.setDayTime(Level.TICKS_PER_DAY * 11 - 1);
        helper.assertTrue(BountyOffers.current(one.server).equals(today), "the board holds until dawn however many ticks pass");
        overworld.setDayTime(Level.TICKS_PER_DAY * 11);
        helper.assertTrue(!BountyOffers.current(one.server).equals(today), "dawn brings new postings");
        overworld.setDayTime(savedDayTime);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void acceptingValidatesOffersAndActiveBounty(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        BlockPos board = standAtABoard(helper, player);
        List<BountyOffers.Offer> offers = BountyOffers.forBoard(player.server, GlobalPos.of(player.level().dimension(), board));
        ResourceLocation unoffered = BountyDefinitions.all().keySet().stream()
                .filter(entityId -> offers.stream().noneMatch(offer -> offer.entityId().equals(entityId))).findFirst().orElseThrow();
        PlayerBounties.accept(player, board, unoffered);
        helper.assertTrue(PlayerBounties.state(player).active().isEmpty(), "an unoffered bounty is refused");

        ResourceLocation offered = offers.get(0).entityId();
        PlayerBounties.accept(player, board, offered);
        BountyState.ActiveBounty active = PlayerBounties.state(player).active().orElseThrow();
        helper.assertTrue(active.entityId().equals(offered) && active.killsRequired() == offers.get(0).definition().kills() && active.killsSoFar() == 0, "accepting an offered bounty starts it");
        PlayerBounties.accept(player, board, offers.get(1).entityId());
        helper.assertTrue(PlayerBounties.state(player).active().orElseThrow().entityId().equals(offered), "a second bounty is refused while one is active");

        PlayerBounties.abandon(player, board);
        BountyState abandoned = PlayerBounties.state(player);
        helper.assertTrue(abandoned.active().isEmpty() && abandoned.claimed().isEmpty(), "abandoning clears the bounty without claiming it");
        PlayerBounties.accept(player, board, offered);
        helper.assertTrue(PlayerBounties.state(player).active().isPresent(), "an abandoned posting can be taken again at once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boardCommandsRerollAndClear(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        List<BountyOffers.Offer> before = BountyOffers.current(player.server);
        PlayerBounties.setState(player, new BountyState(Optional.empty(), BountyOffers.period(player.server), List.of(before.get(0).entityId())));

        CommandSourceStack source = player.createCommandSourceStack().withPermission(Commands.LEVEL_GAMEMASTERS);
        Commands commands = helper.getLevel().getServer().getCommands();
        commands.performPrefixedCommand(source, "bestiary board clear @s");
        helper.assertTrue(PlayerBounties.state(player).canAccept(before.get(0).entityId()), "board clear forgets claimed postings");
        helper.assertTrue(PlayerBounties.state(player).equals(BountyState.NONE), "a cleared player stores nothing");

        int rerollsBefore = BountyBoardData.of(player.server).rerolls();
        commands.performPrefixedCommand(source, "bestiary board reroll");
        helper.assertTrue(BountyBoardData.of(player.server).rerolls() == rerollsBefore + 1, "reroll bumps the world counter");
        List<BountyOffers.Offer> after = BountyOffers.current(player.server);
        helper.assertTrue(after.stream().noneMatch(before::contains), "rerolled postings share nothing with the previous board");
        helper.assertTrue(BountyOffers.current(player.server).equals(after), "the rerolled board is stable until the next change");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void killsAdvanceAndCompleteABounty(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        long period = BountyOffers.period(player.server);
        PlayerBounties.setState(player, BountyState.NONE.onBoard(period).accepted(ZOMBIE, 2));
        DamageSources sources = helper.getLevel().damageSources();
        killZombie(helper, player, sources);
        helper.assertTrue(PlayerBounties.state(player).active().orElseThrow().killsSoFar() == 1, "the first kill advances the bounty");
        killZombie(helper, player, sources);
        BountyState completed = PlayerBounties.state(player);
        helper.assertTrue(completed.active().isEmpty() && completed.boardPeriod() == period && completed.hasClaimed(ZOMBIE), "the last kill completes the bounty and marks the posting claimed");
        helper.assertTrue(!completed.canAccept(ZOMBIE), "a claimed posting can't be taken again on the same board");
        helper.assertTrue(completed.onBoard(period + 1).canAccept(ZOMBIE), "the next board forgets what was claimed");
        helper.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 12, "the iron reward is given, got " + player.getInventory().countItem(Items.IRON_INGOT));
        helper.assertTrue(player.totalExperience == 100, "the experience reward is given, got " + player.totalExperience);
        Progress progress = BestiaryProgress.of(player, ZOMBIE);
        helper.assertTrue(progress.equals(new Progress(12, 1)), "two real kills plus ten bonus kills and one bounty, got " + progress);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bountyStateSurvivesSavingAndDeath(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        BountyState state = new BountyState(Optional.of(new BountyState.ActiveBounty(ZOMBIE, 5, 2)), 7, List.of(new ResourceLocation("minecraft", "skeleton")));
        PlayerBounties.setState(player, state);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer loaded = MockPlayers.inLevel(helper);
        loaded.load(saved);
        helper.assertTrue(PlayerBounties.state(loaded).equals(state), "bounty state round-trips through NBT");
        ServerPlayer respawned = MockPlayers.inLevel(helper);
        respawned.restoreFrom(player, false);
        helper.assertTrue(PlayerBounties.state(respawned).equals(state), "bounty state is copied on death");
        helper.succeed();
    }

    private static void killZombie(GameTestHelper helper, ServerPlayer player, DamageSources sources) {
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        zombie.moveTo(player.getX() + 2, player.getY(), player.getZ());
        helper.getLevel().addFreshEntity(zombie);
        zombie.hurt(sources.playerAttack(player), 1000f);
        helper.assertTrue(zombie.isDeadOrDying(), "the zombie should be dead");
    }
    private static BlockPos standAtABoard(GameTestHelper helper, ServerPlayer player) {
        BlockPos boardPos = new BlockPos(0, 2, 0);
        helper.setBlock(boardPos.below(), Blocks.STONE);
        helper.setBlock(boardPos, BountyFeature.BOUNTY_BOARD.get().defaultBlockState());
        BlockPos absoluteBoard = helper.absolutePos(boardPos);
        player.moveTo(absoluteBoard.getX() + 1.5, absoluteBoard.getY(), absoluteBoard.getZ() + 0.5);
        return absoluteBoard;
    }

    private static Set<ResourceLocation> creatures(List<BountyOffers.Offer> offers) {
        return offers.stream().map(BountyOffers.Offer::entityId).collect(Collectors.toSet());
    }

}
