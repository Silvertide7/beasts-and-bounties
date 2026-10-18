package net.silvertide.beasts_and_bounties.feature.tracking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record TrackingState(Optional<ResourceLocation> creature, long huntEndsAt, long cooldownEndsAt) {
    public static final Codec<TrackingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("creature").forGetter(TrackingState::creature),
            Codec.LONG.optionalFieldOf("hunt_ends_at", 0L).forGetter(TrackingState::huntEndsAt),
            Codec.LONG.optionalFieldOf("cooldown_ends_at", 0L).forGetter(TrackingState::cooldownEndsAt)
    ).apply(instance, TrackingState::new));

    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(creature, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarLong(huntEndsAt);
        buf.writeVarLong(cooldownEndsAt);
    }

    public static TrackingState read(FriendlyByteBuf buf) {
        return new TrackingState(buf.readOptional(FriendlyByteBuf::readResourceLocation), buf.readVarLong(), buf.readVarLong());
    }

    public static TrackingState tracking(ResourceLocation creature, Optional<TrackingState> previous) {
        return new TrackingState(Optional.of(creature), 0, previous.map(TrackingState::cooldownEndsAt).orElse(0L));
    }

    public Optional<TrackingState> stopped(long now) {
        return now < cooldownEndsAt ? Optional.of(new TrackingState(Optional.empty(), 0, cooldownEndsAt)) : Optional.empty();
    }

    public Optional<TrackingState> huntStarted(long now, ActiveTrackingLevel level) {
        if (creature.isEmpty() || now < cooldownEndsAt) return Optional.empty();
        long huntEndsAt = now + level.durationTicks();
        return Optional.of(new TrackingState(creature, huntEndsAt, huntEndsAt + level.cooldownTicks()));
    }

    public boolean isHunting(long now) {
        return now < huntEndsAt;
    }

    public long ticksUntilReady(long now) {
        return Math.max(0, cooldownEndsAt - now);
    }
}
