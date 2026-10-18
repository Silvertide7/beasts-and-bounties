package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.feature.bounty.client.ClientBounties;

import java.util.List;

public final class BountyPayloads {
    private BountyPayloads() {}

    static void register() {
        Network.toServer(AcceptBounty.class, AcceptBounty::encode, AcceptBounty::decode, (payload, player) -> PlayerBounties.accept(player, payload.entityId()));
        Network.toServer(AbandonBounty.class, AbandonBounty::encode, AbandonBounty::decode, (payload, player) -> PlayerBounties.abandon(player));
        Network.toClient(BoardOpened.class, BoardOpened::encode, BoardOpened::decode, payload -> ClientBounties.openBoard(payload));
        Network.toClient(StateSync.class, StateSync::encode, StateSync::decode, payload -> ClientBounties.setState(payload.state()));
        Network.toClient(BountyClaimed.class, BountyClaimed::encode, BountyClaimed::decode, payload -> ClientBounties.claimed(payload.entityId()));
    }

    public record Offer(ResourceLocation entityId, int kills, List<ItemStack> items, int experience, List<Component> skillExperience, List<Component> habitat) {
        void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(entityId);
            buf.writeVarInt(kills);
            buf.writeCollection(items, FriendlyByteBuf::writeItem);
            buf.writeVarInt(experience);
            buf.writeCollection(skillExperience, FriendlyByteBuf::writeComponent);
            buf.writeCollection(habitat, FriendlyByteBuf::writeComponent);
        }

        static Offer read(FriendlyByteBuf buf) {
            return new Offer(buf.readResourceLocation(), buf.readVarInt(), buf.readList(FriendlyByteBuf::readItem), buf.readVarInt(),
                    buf.readList(FriendlyByteBuf::readComponent), buf.readList(FriendlyByteBuf::readComponent));
        }
    }

    public record BoardOpened(List<Offer> offers, long periodEndsAt, BountyState state, boolean refreshOnly) {
        void encode(FriendlyByteBuf buf) {
            buf.writeCollection(offers, (out, offer) -> offer.write(out));
            buf.writeVarLong(periodEndsAt);
            state.write(buf);
            buf.writeBoolean(refreshOnly);
        }

        static BoardOpened decode(FriendlyByteBuf buf) {
            return new BoardOpened(buf.readList(Offer::read), buf.readVarLong(), BountyState.read(buf), buf.readBoolean());
        }
    }

    public record StateSync(BountyState state) {
        void encode(FriendlyByteBuf buf) {
            state.write(buf);
        }

        static StateSync decode(FriendlyByteBuf buf) {
            return new StateSync(BountyState.read(buf));
        }
    }

    public record BountyClaimed(ResourceLocation entityId) {
        void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(entityId);
        }

        static BountyClaimed decode(FriendlyByteBuf buf) {
            return new BountyClaimed(buf.readResourceLocation());
        }
    }

    public record AcceptBounty(ResourceLocation entityId) {
        void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(entityId);
        }

        static AcceptBounty decode(FriendlyByteBuf buf) {
            return new AcceptBounty(buf.readResourceLocation());
        }
    }

    public record AbandonBounty() {
        void encode(FriendlyByteBuf buf) {}

        static AbandonBounty decode(FriendlyByteBuf buf) {
            return new AbandonBounty();
        }
    }
}
