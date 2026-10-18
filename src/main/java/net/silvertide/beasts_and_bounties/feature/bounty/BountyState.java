package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record BountyState(Optional<ActiveBounty> active, long cooldownEndsAt) {
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
            Codec.LONG.optionalFieldOf("cooldown_ends_at", 0L).forGetter(BountyState::cooldownEndsAt)
    ).apply(instance, BountyState::new));
    public static final BountyState NONE = new BountyState(Optional.empty(), 0);

    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(active, (out, bounty) -> bounty.write(out));
        buf.writeVarLong(cooldownEndsAt);
    }

    public static BountyState read(FriendlyByteBuf buf) {
        return new BountyState(buf.readOptional(ActiveBounty::read), buf.readVarLong());
    }

    public boolean canAccept(long now) {
        return active.isEmpty() && now >= cooldownEndsAt;
    }

    public BountyState accepted(ResourceLocation entityId, int killsRequired) {
        return new BountyState(Optional.of(new ActiveBounty(entityId, killsRequired, 0)), cooldownEndsAt);
    }

    public Optional<BountyState> killRecorded(ResourceLocation entityId) {
        return active.filter(bounty -> bounty.entityId().equals(entityId) && !bounty.isComplete())
                .map(bounty -> new BountyState(Optional.of(new ActiveBounty(entityId, bounty.killsRequired(), bounty.killsSoFar() + 1)), cooldownEndsAt));
    }

    public BountyState ended(long now, long cooldownTicks) {
        return new BountyState(Optional.empty(), now + cooldownTicks);
    }

    public BountyState cleared() {
        return new BountyState(Optional.empty(), 0);
    }

    public long ticksUntilReady(long now) {
        return Math.max(0, cooldownEndsAt - now);
    }

    public boolean isEmpty(long now) {
        return active.isEmpty() && ticksUntilReady(now) == 0;
    }
}
