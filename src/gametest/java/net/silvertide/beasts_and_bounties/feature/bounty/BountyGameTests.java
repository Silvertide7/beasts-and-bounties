package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.util.GsonHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
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
    private static final int SHIPPED_BOUNTIES = 38;

    @GameTest(template = "empty")
    public static void everyShippedBountyMatchesAnEntry(GameTestHelper helper) {
        helper.assertTrue(BountyDefinitions.all().size() == SHIPPED_BOUNTIES, "expected " + SHIPPED_BOUNTIES + " bounties, loaded " + BountyDefinitions.all().size());
        BountyDefinitions.all().keySet().forEach(entityId -> helper.assertTrue(BestiaryEntries.get(entityId) != null, entityId + " has a bounty but no entry"));
        BountyDefinition zombie = BountyDefinitions.all().get(ZOMBIE);
        helper.assertTrue(zombie.kills() == 8 && zombie.bonusKills() == 10 && zombie.experienceReward() == 100 && zombie.itemRewards().getFirst().is(Items.IRON_INGOT) && zombie.itemRewards().getFirst().getCount() == 12, "zombie bounty values");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void skillExperienceRewardsParse(GameTestHelper helper) {
        JsonObject json = GsonHelper.parse("""
                {"rewards": [{"type": "pmmo_xp", "xp": "combat/50"}, {"type": "experience", "amount": 10}]}
                """);
        BountyDefinition definition = BountyDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        List<BountyReward.SkillExperience> skills = definition.skillExperienceRewards();
        helper.assertTrue(skills.size() == 1 && skills.getFirst().skill().equals("combat") && skills.getFirst().amount() == 50, "pmmo_xp parses skill/amount");
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
        ServerPlayer one = helper.makeMockServerPlayerInLevel();
        ServerPlayer two = helper.makeMockServerPlayerInLevel();
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
    @SuppressWarnings("removal")
    public static void acceptingValidatesOffersAndActiveBounty(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<BountyOffers.Offer> offers = BountyOffers.current(player.server);
        ResourceLocation unoffered = BountyDefinitions.all().keySet().stream()
                .filter(entityId -> offers.stream().noneMatch(offer -> offer.entityId().equals(entityId))).findFirst().orElseThrow();
        PlayerBounties.accept(player, unoffered);
        helper.assertTrue(PlayerBounties.state(player).active().isEmpty(), "an unoffered bounty is refused");

        ResourceLocation offered = offers.getFirst().entityId();
        PlayerBounties.accept(player, offered);
        BountyState.ActiveBounty active = PlayerBounties.state(player).active().orElseThrow();
        helper.assertTrue(active.entityId().equals(offered) && active.killsRequired() == offers.getFirst().definition().kills() && active.killsSoFar() == 0, "accepting an offered bounty starts it");
        PlayerBounties.accept(player, offers.get(1).entityId());
        helper.assertTrue(PlayerBounties.state(player).active().orElseThrow().entityId().equals(offered), "a second bounty is refused while one is active");

        PlayerBounties.abandon(player);
        BountyState abandoned = PlayerBounties.state(player);
        helper.assertTrue(abandoned.active().isEmpty() && abandoned.claimed().isEmpty(), "abandoning clears the bounty without claiming it");
        PlayerBounties.accept(player, offered);
        helper.assertTrue(PlayerBounties.state(player).active().isPresent(), "an abandoned posting can be taken again at once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void boardCommandsRerollAndClear(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<BountyOffers.Offer> before = BountyOffers.current(player.server);
        player.setData(PlayerBounties.STATE, new BountyState(Optional.empty(), BountyOffers.period(player.server), List.of(before.getFirst().entityId())));

        CommandSourceStack source = player.createCommandSourceStack().withPermission(Commands.LEVEL_GAMEMASTERS);
        Commands commands = helper.getLevel().getServer().getCommands();
        commands.performPrefixedCommand(source, "bestiary board clear @s");
        helper.assertTrue(PlayerBounties.state(player).canAccept(before.getFirst().entityId()), "board clear forgets claimed postings");
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
    @SuppressWarnings("removal")
    public static void killsAdvanceAndCompleteABounty(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        long period = BountyOffers.period(player.server);
        player.setData(PlayerBounties.STATE, BountyState.NONE.onBoard(period).accepted(ZOMBIE, 2));
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
    @SuppressWarnings("removal")
    public static void bountyStateSurvivesSavingAndDeath(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BountyState state = new BountyState(Optional.of(new BountyState.ActiveBounty(ZOMBIE, 5, 2)), 7, List.of(ResourceLocation.withDefaultNamespace("skeleton")));
        player.setData(PlayerBounties.STATE, state);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer loaded = helper.makeMockServerPlayerInLevel();
        loaded.load(saved);
        helper.assertTrue(PlayerBounties.state(loaded).equals(state), "bounty state round-trips through NBT");
        ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(respawned, player, true));
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
}
