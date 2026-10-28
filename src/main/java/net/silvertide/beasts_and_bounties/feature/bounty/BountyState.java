package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record BountyState(Optional<ActiveBounty> active, long boardPeriod, List<ResourceLocation> claimed) {
    public record ActiveBounty(ResourceLocation entityId, int killsRequired, int killsSoFar) {
        static final Codec<ActiveBounty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("entity").forGetter(ActiveBounty::entityId),
                Codec.INT.fieldOf("kills_required").forGetter(ActiveBounty::killsRequired),
                Codec.INT.optionalFieldOf("kills_so_far", 0).forGetter(ActiveBounty::killsSoFar)
        ).apply(instance, ActiveBounty::new));

        void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(entityId);
            buf.writeVarInt(killsRequired);
            buf.writeVarInt(killsSoFar);
        }

        static ActiveBounty read(FriendlyByteBuf buf) {
            return new ActiveBounty(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt());
        }

        public boolean isComplete() {
            return killsSoFar >= killsRequired;
        }
    }

    public static final Codec<BountyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ActiveBounty.CODEC.optionalFieldOf("active").forGetter(BountyState::active),
            Codec.LONG.optionalFieldOf("board_period", 0L).forGetter(BountyState::boardPeriod),
            ResourceLocation.CODEC.listOf().optionalFieldOf("claimed", List.of()).forGetter(BountyState::claimed)
    ).apply(instance, BountyState::new));
    public static final BountyState NONE = new BountyState(Optional.empty(), 0, List.of());

    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(active, (out, bounty) -> bounty.write(out));
        buf.writeVarLong(boardPeriod);
        buf.writeCollection(claimed, FriendlyByteBuf::writeResourceLocation);
    }

    public static BountyState read(FriendlyByteBuf buf) {
        return new BountyState(buf.readOptional(ActiveBounty::read), buf.readVarLong(), buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public BountyState onBoard(long period) {
        return period == boardPeriod ? this : new BountyState(active, period, List.of());
    }

    public boolean hasClaimed(ResourceLocation entityId) {
        return claimed.contains(entityId);
    }

    public boolean canAccept(ResourceLocation entityId) {
        return active.isEmpty() && !hasClaimed(entityId);
    }

    public BountyState accepted(ResourceLocation entityId, int killsRequired) {
        return new BountyState(Optional.of(new ActiveBounty(entityId, killsRequired, 0)), boardPeriod, claimed);
    }

    public Optional<BountyState> killRecorded(ResourceLocation entityId) {
        return active.filter(bounty -> bounty.entityId().equals(entityId) && !bounty.isComplete())
                .map(bounty -> new BountyState(Optional.of(new ActiveBounty(entityId, bounty.killsRequired(), bounty.killsSoFar() + 1)), boardPeriod, claimed));
    }

    public BountyState completed() {
        List<ResourceLocation> claimedNow = active.map(bounty -> Stream.concat(claimed.stream(), Stream.of(bounty.entityId())).toList()).orElse(claimed);
        return new BountyState(Optional.empty(), boardPeriod, claimedNow);
    }

    public BountyState abandoned() {
        return new BountyState(Optional.empty(), boardPeriod, claimed);
    }

    public boolean isEmpty() {
        return active.isEmpty() && claimed.isEmpty();
    }
}
