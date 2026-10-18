package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.core.PlayerData;
import net.silvertide.beasts_and_bounties.core.PlayerDataEvents;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;

import java.util.Optional;

public final class PlayerTracking {
    private static final float HUNT_START_VOLUME = 0.4f;
    private static final PlayerData<TrackingState> STATE = PlayerDataEvents.register(new PlayerData<>("tracking", TrackingState.CODEC));

    private PlayerTracking() {}

    public static Optional<TrackingState> state(Player player) {
        return STATE.get(player);
    }

    public static void setState(Player player, TrackingState state) {
        STATE.set(player, state);
    }

    public static void track(ServerPlayer player, ResourceLocation creature) {
        if (!BestiaryProgress.bonuses(player, creature).hasTracking()) return;
        STATE.set(player, TrackingState.tracking(creature, state(player)));
        sync(player);
    }

    static void stop(ServerPlayer player) {
        state(player).ifPresent(current -> {
            Optional<TrackingState> remaining = current.stopped(player.level().getGameTime());
            if (remaining.isPresent()) STATE.set(player, remaining.get());
            else STATE.remove(player);
            sync(player);
        });
    }

    static void startHunt(ServerPlayer player) {
        Optional<ResourceLocation> creature = state(player).flatMap(TrackingState::creature);
        if (creature.isEmpty()) {
            player.displayClientMessage(Component.translatable("tracking.beasts_and_bounties.not_tracking"), true);
            return;
        }
        int activeLevel = BestiaryProgress.bonuses(player, creature.get()).activeTracking();
        if (activeLevel == 0) {
            BestiaryEntry entry = BestiaryEntries.get(creature.get());
            Component name = entry == null ? ForgeRegistries.ENTITY_TYPES.getValue(creature.get()).getDescription()
                    : entry.displayName(creature.get(), BestiaryProgress.of(player, creature.get()));
            player.displayClientMessage(Component.translatable("tracking.beasts_and_bounties.no_active", name), true);
            return;
        }
        long now = player.level().getGameTime();
        TrackingState current = state(player).orElseThrow();
        if (current.isHunting(now)) return;
        Optional<TrackingState> hunting = current.huntStarted(now, ActiveTrackingLevel.of(activeLevel));
        if (hunting.isEmpty()) {
            long secondsLeft = (current.ticksUntilReady(now) + SharedConstants.TICKS_PER_SECOND - 1) / SharedConstants.TICKS_PER_SECOND;
            player.displayClientMessage(Component.translatable("tracking.beasts_and_bounties.cooldown", secondsLeft), true);
            return;
        }
        STATE.set(player, hunting.get());
        sync(player);
        player.playNotifySound(TrackingSounds.HUNT_START.get(), SoundSource.PLAYERS, HUNT_START_VOLUME, 1f);
        TrackingScanner.scanHunt(player);
    }

    static void clearExpiredCooldown(ServerPlayer player) {
        STATE.remove(player);
        sync(player);
    }

    static void sync(ServerPlayer player) {
        Network.sendIfConnected(player, new TrackingPayloads.StateSync(state(player)));
    }
}
