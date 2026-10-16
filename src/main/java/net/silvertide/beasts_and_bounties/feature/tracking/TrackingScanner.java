package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.Payloads;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;
import net.silvertide.beasts_and_bounties.feature.bestiary.Bonuses;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class TrackingScanner {
    private TrackingScanner() {}

    @SubscribeEvent
    static void syncOnJoin(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) PlayerTracking.sync(event.getPlayer());
    }

    @SubscribeEvent
    static void scan(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TrackingState state = player.getExistingDataOrNull(PlayerTracking.STATE);
        if (state == null) return;
        long now = player.level().getGameTime();
        if (state.creature().isEmpty()) {
            if (now >= state.cooldownEndsAt()) PlayerTracking.clearExpiredCooldown(player);
            return;
        }
        if (state.isHunting(now)) {
            if (isScanTick(now, player, ActiveTrackingLevel.scanIntervalTicks())) scanHunt(player);
            return;
        }
        endHunt(player);
        if (!isScanTick(now, player, SharedConstants.TICKS_PER_SECOND)) return;

        ResourceLocation creature = state.creature().get();
        Bonuses bonuses = BestiaryProgress.bonuses(player, creature);
        if (!bonuses.hasTracking()) {
            PlayerTracking.stop(player);
            return;
        }
        if (bonuses.passiveTracking() == 0) return;
        PassiveTrackingLevel passive = PassiveTrackingLevel.of(bonuses.passiveTracking());
        if (isScanTick(now, player, passive.pingIntervalTicks()))
            passivePing(player, creature, passive).ifPresent(ping -> Payloads.sendIfConnected(player, ping));
    }

    static void scanHunt(ServerPlayer player) {
        PlayerTracking.state(player).flatMap(TrackingState::creature).ifPresent(creature -> {
            int activeLevel = BestiaryProgress.bonuses(player, creature).activeTracking();
            sendIfChanged(player, activeLevel == 0 ? TrackingPayloads.HuntUpdate.NONE : hunt(player, creature, ActiveTrackingLevel.of(activeLevel)));
        });
    }

    static Optional<TrackingPayloads.PassivePing> passivePing(Player player, ResourceLocation creature, PassiveTrackingLevel level) {
        return nearbySortedByDistance(player, creature, level.range()).stream().findFirst().map(nearest -> {
            TrackingDirection direction = TrackingDirection.fromRelativeYaw(
                    TrackingDirection.yawTowards(player.getX(), player.getZ(), nearest.getX(), nearest.getZ()) - player.getYRot(),
                    level.directions());
            Optional<TrackingHeight> height = TrackingHeight.of(nearest.getY() - player.getY());
            Optional<Integer> distance = level.showsDistance() ? Optional.of(roundedDistance(player.distanceTo(nearest))) : Optional.empty();
            Optional<Vec3> wispTarget = level.showsWispTrail() ? Optional.of(nearest.getBoundingBox().getCenter()) : Optional.empty();
            return new TrackingPayloads.PassivePing(creature, level.level(), direction, height, distance, wispTarget);
        });
    }

    static TrackingPayloads.HuntUpdate hunt(Player player, ResourceLocation creature, ActiveTrackingLevel level) {
        List<LivingEntity> nearby = nearbySortedByDistance(player, creature, level.arrowRange());
        if (nearby.isEmpty()) return TrackingPayloads.HuntUpdate.NONE;
        LivingEntity nearest = nearby.getFirst();
        double glowRangeSquared = (double) ActiveTrackingLevel.glowRange() * ActiveTrackingLevel.glowRange();
        List<Integer> glowIds = switch (level.glow()) {
            case NONE -> List.of();
            case NEAREST -> player.distanceToSqr(nearest) <= glowRangeSquared ? List.of(nearest.getId()) : List.of();
            case ALL -> nearby.stream().filter(entity -> player.distanceToSqr(entity) <= glowRangeSquared).map(Entity::getId).sorted().toList();
        };
        return new TrackingPayloads.HuntUpdate(Optional.of(new TrackingPayloads.HuntTarget(nearest.getId(), nearest.position())), glowIds);
    }

    static int roundedDistance(double distance) {
        int rounding = TrackingConfig.DISTANCE_ROUNDING_BLOCKS.get();
        return Math.max(rounding, (int) Math.round(distance / rounding) * rounding);
    }

    private static List<LivingEntity> nearbySortedByDistance(Player player, ResourceLocation creature, int range) {
        return nearbyOfType(player, BuiltInRegistries.ENTITY_TYPE.get(creature), range);
    }

    private static <T extends Entity> List<LivingEntity> nearbyOfType(Player player, EntityType<T> type, int range) {
        double rangeSquared = (double) range * range;
        return player.level().getEntities(type, player.getBoundingBox().inflate(range),
                        entity -> entity.isAlive() && player.distanceToSqr(entity) <= rangeSquared).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .toList();
    }

    private static void endHunt(ServerPlayer player) {
        if (player.getData(PlayerTracking.LAST_HUNT_UPDATE) != TrackingPayloads.HuntUpdate.NONE) sendIfChanged(player, TrackingPayloads.HuntUpdate.NONE);
    }

    private static void sendIfChanged(ServerPlayer player, TrackingPayloads.HuntUpdate update) {
        if (player.getData(PlayerTracking.LAST_HUNT_UPDATE).needsNoResendFor(update)) return;
        player.setData(PlayerTracking.LAST_HUNT_UPDATE, update);
        Payloads.sendIfConnected(player, update);
    }

    private static boolean isScanTick(long now, Player player, int interval) {
        return (now + player.getId()) % interval == 0;
    }
}
