package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.SharedConstants;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;
import net.silvertide.beasts_and_bounties.feature.bestiary.Bonuses;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class TrackingScanner {
    private static final Map<UUID, TrackingPayloads.HuntUpdate> LAST_HUNT_UPDATES = new HashMap<>();

    private TrackingScanner() {}

    @SubscribeEvent
    public static void syncOnJoin(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) PlayerTracking.sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void forgetLastHuntUpdateOnLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HUNT_UPDATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void scan(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        TrackingState state = PlayerTracking.state(player).orElse(null);
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
            passivePing(player, creature, passive).ifPresent(ping -> Network.sendIfConnected(player, ping));
    }

    static void scanHunt(ServerPlayer player) {
        PlayerTracking.state(player).flatMap(TrackingState::creature).ifPresent(creature -> {
            int activeLevel = BestiaryProgress.bonuses(player, creature).activeTracking();
            sendIfChanged(player, activeLevel == 0 ? TrackingPayloads.HuntUpdate.NONE : hunt(player, creature, ActiveTrackingLevel.of(activeLevel)));
        });
    }

    public static Optional<TrackingPayloads.PassivePing> passivePing(Player player, ResourceLocation creature, PassiveTrackingLevel level) {
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

    public static TrackingPayloads.HuntUpdate hunt(Player player, ResourceLocation creature, ActiveTrackingLevel level) {
        List<LivingEntity> nearby = nearbySortedByDistance(player, creature, level.arrowRange());
        if (nearby.isEmpty()) return TrackingPayloads.HuntUpdate.NONE;
        LivingEntity nearest = nearby.get(0);
        double glowRangeSquared = (double) ActiveTrackingLevel.glowRange() * ActiveTrackingLevel.glowRange();
        List<Integer> glowIds = switch (level.glow()) {
            case NONE -> List.of();
            case NEAREST -> player.distanceToSqr(nearest) <= glowRangeSquared ? List.of(nearest.getId()) : List.of();
            case ALL -> nearby.stream().filter(entity -> player.distanceToSqr(entity) <= glowRangeSquared).map(Entity::getId).sorted().toList();
        };
        return new TrackingPayloads.HuntUpdate(Optional.of(new TrackingPayloads.HuntTarget(nearest.getId(), nearest.position())), glowIds);
    }

    public static int roundedDistance(double distance) {
        int rounding = TrackingConfig.DISTANCE_ROUNDING_BLOCKS.get();
        return Math.max(rounding, (int) Math.round(distance / rounding) * rounding);
    }

    private static List<LivingEntity> nearbySortedByDistance(Player player, ResourceLocation creature, int range) {
        return nearbyOfType(player, ForgeRegistries.ENTITY_TYPES.getValue(creature), range);
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
        if (lastHuntUpdate(player) != TrackingPayloads.HuntUpdate.NONE) sendIfChanged(player, TrackingPayloads.HuntUpdate.NONE);
    }

    private static TrackingPayloads.HuntUpdate lastHuntUpdate(ServerPlayer player) {
        return LAST_HUNT_UPDATES.getOrDefault(player.getUUID(), TrackingPayloads.HuntUpdate.NONE);
    }

    private static void sendIfChanged(ServerPlayer player, TrackingPayloads.HuntUpdate update) {
        if (lastHuntUpdate(player).needsNoResendFor(update)) return;
        LAST_HUNT_UPDATES.put(player.getUUID(), update);
        Network.sendIfConnected(player, update);
    }

    private static boolean isScanTick(long now, Player player, int interval) {
        return (now + player.getId()) % interval == 0;
    }
}
