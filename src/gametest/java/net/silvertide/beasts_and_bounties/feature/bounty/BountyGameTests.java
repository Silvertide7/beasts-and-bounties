package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;
import net.silvertide.beasts_and_bounties.feature.bestiary.Progress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        helper.assertTrue(zombie.kills() == 5 && zombie.bonusKills() == 4 && zombie.experienceReward() == 60 && zombie.itemRewards().getFirst().is(Items.EMERALD), "zombie bounty values");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void offersAreStableWithinAPeriodAndChangeAcrossPeriods(GameTestHelper helper) {
        UUID playerId = UUID.nameUUIDFromBytes("bounty-test".getBytes());
        List<BountyOffers.Offer> first = BountyOffers.pick(playerId, 42, 1, 0, 3, BountyDefinitions.all());
        List<BountyOffers.Offer> again = BountyOffers.pick(playerId, 42, 1, 0, 3, BountyDefinitions.all());
        helper.assertTrue(first.size() == 3, "three offers are shown");
        helper.assertTrue(first.stream().map(BountyOffers.Offer::entityId).distinct().count() == 3, "offers are distinct");
        helper.assertTrue(first.equals(again), "the same period gives the same offers");
        boolean anyDifferent = false;
        for (long period = 2; period <= 6; period++) anyDifferent |= !BountyOffers.pick(playerId, 42, period, 0, 3, BountyDefinitions.all()).equals(first);
        helper.assertTrue(anyDifferent, "later periods give different offers");
        boolean anyRerollDiffers = false;
        for (int rerolls = 1; rerolls <= 5; rerolls++) anyRerollDiffers |= !BountyOffers.pick(playerId, 42, 1, rerolls, 3, BountyDefinitions.all()).equals(first);
        helper.assertTrue(anyRerollDiffers, "a reroll gives different offers in the same period");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void acceptingValidatesOffersActiveBountyAndCooldown(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        long now = helper.getLevel().getGameTime();
        List<BountyOffers.Offer> offers = BountyOffers.forPlayer(player, now);
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
        helper.assertTrue(abandoned.active().isEmpty() && abandoned.ticksUntilReady(now) > 0, "abandoning clears the bounty and starts a cooldown");
        PlayerBounties.accept(player, offered);
        helper.assertTrue(PlayerBounties.state(player).active().isEmpty(), "nothing can be accepted during the cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void killsAdvanceAndCompleteABounty(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setData(PlayerBounties.STATE, BountyState.NONE.accepted(ZOMBIE, 2));
        DamageSources sources = helper.getLevel().damageSources();
        killZombie(helper, player, sources);
        helper.assertTrue(PlayerBounties.state(player).active().orElseThrow().killsSoFar() == 1, "the first kill advances the bounty");
        killZombie(helper, player, sources);
        BountyState completed = PlayerBounties.state(player);
        helper.assertTrue(completed.active().isEmpty() && completed.ticksUntilReady(helper.getLevel().getGameTime()) > 0, "the last kill completes the bounty and starts the claim cooldown");
        helper.assertTrue(player.getInventory().countItem(Items.EMERALD) == 3, "the emerald reward is given, got " + player.getInventory().countItem(Items.EMERALD));
        helper.assertTrue(player.totalExperience == 60, "the experience reward is given, got " + player.totalExperience);
        Progress progress = BestiaryProgress.of(player, ZOMBIE);
        helper.assertTrue(progress.equals(new Progress(6, 1)), "two real kills plus four bonus kills and one bounty, got " + progress);
        helper.succeed();
    }

    @GameTest(template = "empty")
    @SuppressWarnings("removal")
    public static void bountyStateSurvivesSavingAndDeath(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BountyState state = new BountyState(Optional.of(new BountyState.ActiveBounty(ZOMBIE, 5, 2)), 900, 1);
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
