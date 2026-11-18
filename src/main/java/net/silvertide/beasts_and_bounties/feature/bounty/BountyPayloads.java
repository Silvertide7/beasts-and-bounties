package net.silvertide.beasts_and_bounties.feature.bounty;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bounty.client.ClientBounties;

import java.util.List;

public final class BountyPayloads {
    private BountyPayloads() {}

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(AcceptBounty.TYPE, AcceptBounty.STREAM_CODEC, (payload, context) -> PlayerBounties.accept((ServerPlayer) context.player(), payload.board(), payload.entityId()));
        registrar.playToServer(AbandonBounty.TYPE, AbandonBounty.STREAM_CODEC, (payload, context) -> PlayerBounties.abandon((ServerPlayer) context.player(), payload.board()));
        registrar.playToClient(BoardOpened.TYPE, BoardOpened.STREAM_CODEC, (payload, context) -> ClientBounties.openBoard(payload));
        registrar.playToClient(StateSync.TYPE, StateSync.STREAM_CODEC, (payload, context) -> ClientBounties.setState(payload.state()));
        registrar.playToClient(BountyClaimed.TYPE, BountyClaimed.STREAM_CODEC, (payload, context) -> ClientBounties.claimed(payload.entityId()));
    }

    public record Offer(ResourceLocation entityId, int kills, List<ItemStack> items, int experience, List<Component> skillExperience, List<Component> habitat) {
        static final StreamCodec<RegistryFriendlyByteBuf, Offer> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Offer::entityId,
                ByteBufCodecs.VAR_INT, Offer::kills,
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Offer::items,
                ByteBufCodecs.VAR_INT, Offer::experience,
                ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), Offer::skillExperience,
                ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list()), Offer::habitat,
                Offer::new);
    }

    public record BoardOpened(BlockPos board, List<Offer> offers, int dawnsUntilRefresh, BountyState state, boolean refreshOnly) implements CustomPacketPayload {
        static final Type<BoardOpened> TYPE = new Type<>(BeastsAndBountiesMod.id("board_opened"));
        static final StreamCodec<RegistryFriendlyByteBuf, BoardOpened> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BoardOpened::board,
                Offer.STREAM_CODEC.apply(ByteBufCodecs.list()), BoardOpened::offers,
                ByteBufCodecs.VAR_INT, BoardOpened::dawnsUntilRefresh,
                BountyState.STREAM_CODEC, BoardOpened::state,
                ByteBufCodecs.BOOL, BoardOpened::refreshOnly,
                BoardOpened::new);

        @Override
        public Type<BoardOpened> type() {
            return TYPE;
        }
    }

    public record StateSync(BountyState state) implements CustomPacketPayload {
        static final Type<StateSync> TYPE = new Type<>(BeastsAndBountiesMod.id("bounty_state"));
        static final StreamCodec<ByteBuf, StateSync> STREAM_CODEC = BountyState.STREAM_CODEC.map(StateSync::new, StateSync::state);

        @Override
        public Type<StateSync> type() {
            return TYPE;
        }
    }

    public record BountyClaimed(ResourceLocation entityId) implements CustomPacketPayload {
        static final Type<BountyClaimed> TYPE = new Type<>(BeastsAndBountiesMod.id("bounty_claimed"));
        static final StreamCodec<ByteBuf, BountyClaimed> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(BountyClaimed::new, BountyClaimed::entityId);

        @Override
        public Type<BountyClaimed> type() {
            return TYPE;
        }
    }

    public record AcceptBounty(BlockPos board, ResourceLocation entityId) implements CustomPacketPayload {
        static final Type<AcceptBounty> TYPE = new Type<>(BeastsAndBountiesMod.id("accept_bounty"));
        static final StreamCodec<ByteBuf, AcceptBounty> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, AcceptBounty::board,
                ResourceLocation.STREAM_CODEC, AcceptBounty::entityId,
                AcceptBounty::new);

        @Override
        public Type<AcceptBounty> type() {
            return TYPE;
        }
    }

    public record AbandonBounty(BlockPos board) implements CustomPacketPayload {
        static final Type<AbandonBounty> TYPE = new Type<>(BeastsAndBountiesMod.id("abandon_bounty"));
        static final StreamCodec<ByteBuf, AbandonBounty> STREAM_CODEC = BlockPos.STREAM_CODEC.map(AbandonBounty::new, AbandonBounty::board);

        @Override
        public Type<AbandonBounty> type() {
            return TYPE;
        }
    }
}
