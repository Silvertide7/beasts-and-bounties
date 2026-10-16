package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.silvertide.beasts_and_bounties.core.Payloads;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;

import java.util.Optional;
import java.util.function.Supplier;

public final class PlayerTracking {
    private static final float HUNT_START_VOLUME = 0.4f;

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BeastsAndBountiesMod.MODID);

    static final Supplier<AttachmentType<TrackingState>> STATE =
            ATTACHMENT_TYPES.register("tracking",
                    () -> AttachmentType.builder(() -> new TrackingState(Optional.empty(), 0, 0))
                            .serialize(TrackingState.CODEC)
                            .copyOnDeath()
                            .build());

    static final Supplier<AttachmentType<TrackingPayloads.HuntUpdate>> LAST_HUNT_UPDATE =
            ATTACHMENT_TYPES.register("last_hunt_update",
                    () -> AttachmentType.builder(() -> TrackingPayloads.HuntUpdate.NONE).build());

    private PlayerTracking() {}

    static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static Optional<TrackingState> state(Player player) {
        return Optional.ofNullable(player.getExistingDataOrNull(STATE));
    }

    static void track(ServerPlayer player, ResourceLocation creature) {
        if (!BestiaryProgress.bonuses(player, creature).hasTracking()) return;
        player.setData(STATE, TrackingState.tracking(creature, state(player)));
        sync(player);
    }

    static void stop(ServerPlayer player) {
        state(player).ifPresent(current -> {
            Optional<TrackingState> remaining = current.stopped(player.level().getGameTime());
            if (remaining.isPresent()) player.setData(STATE, remaining.get());
            else player.removeData(STATE);
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
            Component name = entry == null ? BuiltInRegistries.ENTITY_TYPE.get(creature.get()).getDescription()
                    : entry.displayName(creature.get(), BestiaryProgress.of(player, creature.get()));
            player.displayClientMessage(Component.translatable("tracking.beasts_and_bounties.no_active", name), true);
            return;
        }
        long now = player.level().getGameTime();
        TrackingState current = state(player).orElseThrow();
        if (current.isHunting(now)) return;
        Optional<TrackingState> hunting = current.huntStarted(now, ActiveTrackingLevel.of(activeLevel));
        if (hunting.isEmpty()) {
            long secondsLeft = Math.ceilDiv(current.ticksUntilReady(now), SharedConstants.TICKS_PER_SECOND);
            player.displayClientMessage(Component.translatable("tracking.beasts_and_bounties.cooldown", secondsLeft), true);
            return;
        }
        player.setData(STATE, hunting.get());
        sync(player);
        player.playNotifySound(TrackingSounds.HUNT_START.get(), SoundSource.PLAYERS, HUNT_START_VOLUME, 1f);
        TrackingScanner.scanHunt(player);
    }

    static void clearExpiredCooldown(ServerPlayer player) {
        player.removeData(STATE);
        sync(player);
    }

    static void sync(ServerPlayer player) {
        Payloads.sendIfConnected(player, new TrackingPayloads.StateSync(state(player)));
    }
}
