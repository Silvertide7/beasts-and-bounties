package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record BountyState(Optional<ActiveBounty> active, long cooldownEndsAt, int rerolls) {
    public record ActiveBounty(ResourceLocation entityId, int killsRequired, int killsSoFar) {
        static final Codec<ActiveBounty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("entity").forGetter(ActiveBounty::entityId),
                Codec.INT.fieldOf("kills_required").forGetter(ActiveBounty::killsRequired),
                Codec.INT.optionalFieldOf("kills_so_far", 0).forGetter(ActiveBounty::killsSoFar)
        ).apply(instance, ActiveBounty::new));
        static final StreamCodec<ByteBuf, ActiveBounty> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, ActiveBounty::entityId,
                ByteBufCodecs.VAR_INT, ActiveBounty::killsRequired,
                ByteBufCodecs.VAR_INT, ActiveBounty::killsSoFar,
                ActiveBounty::new);

        public boolean isComplete() {
            return killsSoFar >= killsRequired;
        }
    }

    public static final Codec<BountyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ActiveBounty.CODEC.optionalFieldOf("active").forGetter(BountyState::active),
            Codec.LONG.optionalFieldOf("cooldown_ends_at", 0L).forGetter(BountyState::cooldownEndsAt),
            Codec.INT.optionalFieldOf("rerolls", 0).forGetter(BountyState::rerolls)
    ).apply(instance, BountyState::new));
    public static final StreamCodec<ByteBuf, BountyState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ActiveBounty.STREAM_CODEC), BountyState::active,
            ByteBufCodecs.VAR_LONG, BountyState::cooldownEndsAt,
            ByteBufCodecs.VAR_INT, BountyState::rerolls,
            BountyState::new);
    public static final BountyState NONE = new BountyState(Optional.empty(), 0, 0);

    public boolean canAccept(long now) {
        return active.isEmpty() && now >= cooldownEndsAt;
    }

    public BountyState accepted(ResourceLocation entityId, int killsRequired) {
        return new BountyState(Optional.of(new ActiveBounty(entityId, killsRequired, 0)), cooldownEndsAt, rerolls);
    }

    public Optional<BountyState> killRecorded(ResourceLocation entityId) {
        return active.filter(bounty -> bounty.entityId().equals(entityId) && !bounty.isComplete())
                .map(bounty -> new BountyState(Optional.of(new ActiveBounty(entityId, bounty.killsRequired(), bounty.killsSoFar() + 1)), cooldownEndsAt, rerolls));
    }

    public BountyState ended(long now, long cooldownTicks) {
        return new BountyState(Optional.empty(), now + cooldownTicks, rerolls);
    }

    public BountyState rerolled() {
        return new BountyState(active, cooldownEndsAt, rerolls + 1);
    }

    public BountyState cleared() {
        return new BountyState(Optional.empty(), 0, rerolls);
    }

    public long ticksUntilReady(long now) {
        return Math.max(0, cooldownEndsAt - now);
    }

    public boolean isEmpty(long now) {
        return active.isEmpty() && ticksUntilReady(now) == 0 && rerolls == 0;
    }
}
