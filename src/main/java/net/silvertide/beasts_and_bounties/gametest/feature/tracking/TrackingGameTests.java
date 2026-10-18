package net.silvertide.beasts_and_bounties.gametest.feature.tracking;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.gametest.MockPlayers;
import net.silvertide.beasts_and_bounties.feature.tracking.ActiveTrackingLevel;
import net.silvertide.beasts_and_bounties.feature.tracking.PassiveTrackingLevel;
import net.silvertide.beasts_and_bounties.feature.tracking.PlayerTracking;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingDirection;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingHeight;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingPayloads;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingScanner;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingState;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.Bonuses;
import net.silvertide.beasts_and_bounties.feature.bestiary.Progress;

import java.util.List;
import java.util.Optional;

@GameTestHolder(BeastsAndBountiesMod.MODID)
@PrefixGameTestTemplate(false)
public class TrackingGameTests {
    private static final ResourceLocation ZOMBIE = EntityType.getKey(EntityType.ZOMBIE);

    @GameTest(template = "empty")
    public static void trackingUnlocksArePrecomputedPerMilestone(GameTestHelper helper) {
        BestiaryEntry skeleton = BestiaryEntries.get(EntityType.SKELETON);
        Bonuses before = skeleton.bonuses(new Progress(64, 0));
        Bonuses scent = skeleton.bonuses(new Progress(65, 0));
        Bonuses firstHunt = skeleton.bonuses(new Progress(220, 0));
        Bonuses gated = skeleton.bonuses(new Progress(500, 0));
        Bonuses oneBounty = skeleton.bonuses(new Progress(500, 1));
        Bonuses complete = skeleton.bonuses(new Progress(500, 2));
        helper.assertTrue(before.passiveTracking() == 0 && !before.hasTracking(), "no tracking before 65 kills");
        helper.assertTrue(scent.passiveTracking() == 1 && scent.activeTracking() == 0, "passive 1 at 65 kills");
        helper.assertTrue(firstHunt.passiveTracking() == 2 && firstHunt.activeTracking() == 1, "passive 2 and active 1 at 220 kills");
        helper.assertTrue(gated.passiveTracking() == 2 && gated.activeTracking() == 2, "passive 3 waits for a bounty however many kills");
        helper.assertTrue(oneBounty.passiveTracking() == 3 && oneBounty.activeTracking() == 2, "one bounty unlocks passive 3, active 3 needs a second");
        helper.assertTrue(complete.passiveTracking() == 3 && complete.activeTracking() == 3, "both reach 3 when complete");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stoppingOrSwitchingNeverResetsTheCooldown(GameTestHelper helper) {
        TrackingState tracking = TrackingState.tracking(ZOMBIE, Optional.empty());
        long huntEnds = 100 + ActiveTrackingLevel.HUNT.durationTicks();
        long cooldownEnds = huntEnds + ActiveTrackingLevel.HUNT.cooldownTicks();
        TrackingState hunting = tracking.huntStarted(100, ActiveTrackingLevel.HUNT).orElseThrow();
        helper.assertTrue(hunting.huntEndsAt() == huntEnds && hunting.cooldownEndsAt() == cooldownEnds, "a hunt runs for its duration and then its cooldown");
        helper.assertTrue(hunting.isHunting(huntEnds - 1) && !hunting.isHunting(huntEnds), "hunt lasts exactly its duration");
        helper.assertTrue(hunting.huntStarted(huntEnds + 1, ActiveTrackingLevel.HUNT).isEmpty(), "can't hunt again during the cooldown");

        TrackingState stopped = hunting.stopped(huntEnds + 1).orElseThrow();
        helper.assertTrue(stopped.creature().isEmpty() && stopped.cooldownEndsAt() == cooldownEnds, "stop keeps the cooldown");
        TrackingState switched = TrackingState.tracking(EntityType.getKey(EntityType.SKELETON), Optional.of(stopped));
        helper.assertTrue(switched.cooldownEndsAt() == cooldownEnds && switched.huntEndsAt() == 0, "tracking again inherits the cooldown");
        helper.assertTrue(switched.huntStarted(cooldownEnds, ActiveTrackingLevel.MARK).isPresent(), "hunting works once the cooldown ends");
        helper.assertTrue(hunting.stopped(cooldownEnds).isEmpty(), "stopping after the cooldown leaves nothing behind");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void directionsAreRelativeToFacingAndDistancesRoundToTen(GameTestHelper helper) {
        helper.assertTrue(TrackingDirection.fromRelativeYaw(0, 4) == TrackingDirection.AHEAD, "0 is ahead");
        helper.assertTrue(TrackingDirection.fromRelativeYaw(90, 4) == TrackingDirection.RIGHT, "+90 is right");
        helper.assertTrue(TrackingDirection.fromRelativeYaw(-90, 4) == TrackingDirection.LEFT, "-90 is left");
        helper.assertTrue(TrackingDirection.fromRelativeYaw(180, 4) == TrackingDirection.BEHIND, "180 is behind");
        helper.assertTrue(TrackingDirection.fromRelativeYaw(44, 8) == TrackingDirection.AHEAD_RIGHT, "8-way ahead-right");
        helper.assertTrue(TrackingDirection.fromRelativeYaw(-135, 8) == TrackingDirection.BEHIND_LEFT, "8-way behind-left");
        float towardsEastWhileFacingSouth = TrackingDirection.yawTowards(0, 0, 1, 0);
        helper.assertTrue(TrackingDirection.fromRelativeYaw(towardsEastWhileFacingSouth, 4) == TrackingDirection.LEFT, "east is on your left when facing south");
        helper.assertTrue(TrackingScanner.roundedDistance(3) == 10 && TrackingScanner.roundedDistance(26) == 30 && TrackingScanner.roundedDistance(34) == 30, "distances round to the nearest 10, minimum 10");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scansFindTheNearestAndGlowByLevel(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        Vec3 origin = player.position();
        player.setYRot(0);
        helper.getLevel().getEntities(EntityType.ZOMBIE, player.getBoundingBox().inflate(80), zombie -> true).forEach(Zombie::discard);
        Zombie near = stillZombie(helper, origin.add(5, 0, 0));
        Zombie middle = stillZombie(helper, origin.add(0, 0, 10));
        Zombie far = stillZombie(helper, origin.add(0, 0, 40));

        TrackingPayloads.PassivePing scent = TrackingScanner.passivePing(player, ZOMBIE, PassiveTrackingLevel.SCENT).orElseThrow();
        helper.assertTrue(scent.direction() == TrackingDirection.LEFT && scent.roundedDistance().isEmpty() && scent.wispTarget().isEmpty(),
                "P1 gives a 4-way direction only, got " + scent);
        helper.assertTrue(scent.height().isEmpty(), "level ground gives no above/below hint");
        TrackingPayloads.PassivePing trail = TrackingScanner.passivePing(player, ZOMBIE, PassiveTrackingLevel.TRAIL).orElseThrow();
        helper.assertTrue(trail.level() == 2 && trail.roundedDistance().orElseThrow() == 10 && trail.wispTarget().isEmpty(), "P2 adds the distance but no wisp");
        TrackingPayloads.PassivePing track = TrackingScanner.passivePing(player, ZOMBIE, PassiveTrackingLevel.TRACK).orElseThrow();
        helper.assertTrue(track.roundedDistance().orElseThrow() == 10, "P3 gives the distance rounded to 10");
        helper.assertTrue(track.direction() == TrackingDirection.LEFT && track.wispTarget().orElseThrow().distanceTo(near.getBoundingBox().getCenter()) < 0.01,
                "P3 sends the wisp toward the nearest one only, got " + track);
        helper.assertTrue(TrackingHeight.of(5).equals(Optional.of(TrackingHeight.ABOVE)) && TrackingHeight.of(-5).equals(Optional.of(TrackingHeight.BELOW)),
                "more than 4 blocks off reads as above or below");

        assertHunt(helper, TrackingScanner.hunt(player, ZOMBIE, ActiveTrackingLevel.HUNT), near, List.of());
        assertHunt(helper, TrackingScanner.hunt(player, ZOMBIE, ActiveTrackingLevel.SIGHT), near, List.of(near.getId()));
        assertHunt(helper, TrackingScanner.hunt(player, ZOMBIE, ActiveTrackingLevel.MARK), near, List.of(near.getId(), middle.getId()));

        near.discard();
        middle.discard();
        helper.assertTrue(TrackingScanner.passivePing(player, ZOMBIE, PassiveTrackingLevel.SCENT).isEmpty(), "40 blocks is out of P1 range");
        helper.assertTrue(TrackingScanner.passivePing(player, ZOMBIE, PassiveTrackingLevel.TRACK).isPresent(), "40 blocks is within P3 range");
        far.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trackingNeedsAnUnlockAndCleansUpAfterItself(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        ResourceLocation skeleton = EntityType.getKey(EntityType.SKELETON);
        PlayerTracking.track(player, skeleton);
        helper.assertTrue(PlayerTracking.state(player).isEmpty(), "tracking a creature with nothing unlocked is refused");

        BestiaryProgress.all(player).put(skeleton, new Progress(65, 0));
        PlayerTracking.track(player, skeleton);
        helper.assertTrue(PlayerTracking.state(player).flatMap(TrackingState::creature).equals(Optional.of(skeleton)), "tracking starts once unlocked");

        BestiaryProgress.all(player).clear();
        helper.onEachTick(() -> MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player)));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(PlayerTracking.state(player).isEmpty(), "tracking stops within a second once the unlock is gone");
            long now = helper.getLevel().getGameTime();
            PlayerTracking.setState(player, new TrackingState(Optional.empty(), 0, now + 2));
            helper.succeedWhen(() -> helper.assertTrue(PlayerTracking.state(player).isEmpty(), "an expired cooldown copy is removed"));
        });
    }

    @GameTest(template = "empty")
    public static void trackingStateSurvivesSavingAndDeath(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        TrackingState state = new TrackingState(Optional.of(ZOMBIE), 100, 2700);
        PlayerTracking.setState(player, state);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer loaded = MockPlayers.inLevel(helper);
        loaded.load(saved);
        helper.assertTrue(PlayerTracking.state(loaded).equals(Optional.of(state)), "tracking state round-trips through NBT");
        ServerPlayer respawned = MockPlayers.inLevel(helper);
        respawned.restoreFrom(player, false);
        helper.assertTrue(PlayerTracking.state(respawned).equals(Optional.of(state)), "tracking state is copied on death");
        helper.succeed();
    }

    private static Zombie stillZombie(GameTestHelper helper, Vec3 position) {
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        zombie.setNoAi(true);
        zombie.moveTo(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static void assertHunt(GameTestHelper helper, TrackingPayloads.HuntUpdate update, Zombie expectedTarget, List<Integer> expectedGlow) {
        helper.assertTrue(update.target().orElseThrow().entityId() == expectedTarget.getId(), "arrow should point at the nearest zombie");
        helper.assertTrue(update.glowIds().equals(expectedGlow), "expected glow " + expectedGlow + ", got " + update.glowIds());
    }
}
