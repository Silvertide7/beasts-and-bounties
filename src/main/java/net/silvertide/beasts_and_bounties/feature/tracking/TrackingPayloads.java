package net.silvertide.beasts_and_bounties.feature.tracking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.tracking.client.ClientTracking;

import java.util.List;
import java.util.Optional;

public final class TrackingPayloads {
    private TrackingPayloads() {}

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetTracked.TYPE, SetTracked.STREAM_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            payload.creature().ifPresentOrElse(creature -> PlayerTracking.track(player, creature), () -> PlayerTracking.stop(player));
        });
        registrar.playToServer(StartHunt.TYPE, StartHunt.STREAM_CODEC, (payload, context) -> PlayerTracking.startHunt((ServerPlayer) context.player()));
        registrar.playToClient(StateSync.TYPE, StateSync.STREAM_CODEC, (payload, context) -> ClientTracking.setState(payload.state()));
        registrar.playToClient(PassivePing.TYPE, PassivePing.STREAM_CODEC, (payload, context) -> ClientTracking.receivePing(payload));
        registrar.playToClient(HuntUpdate.TYPE, HuntUpdate.STREAM_CODEC, (payload, context) -> ClientTracking.receiveHunt(payload));
    }

    public record SetTracked(Optional<ResourceLocation> creature) implements CustomPacketPayload {
        static final Type<SetTracked> TYPE = new Type<>(BeastsAndBountiesMod.id("set_tracked"));
        static final StreamCodec<ByteBuf, SetTracked> STREAM_CODEC =
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).map(SetTracked::new, SetTracked::creature);

        @Override
        public Type<SetTracked> type() {
            return TYPE;
        }
    }

    public record StartHunt() implements CustomPacketPayload {
        static final Type<StartHunt> TYPE = new Type<>(BeastsAndBountiesMod.id("start_hunt"));
        static final StreamCodec<ByteBuf, StartHunt> STREAM_CODEC = StreamCodec.unit(new StartHunt());

        @Override
        public Type<StartHunt> type() {
            return TYPE;
        }
    }

    public record StateSync(Optional<TrackingState> state) implements CustomPacketPayload {
        static final Type<StateSync> TYPE = new Type<>(BeastsAndBountiesMod.id("tracking_state"));
        static final StreamCodec<ByteBuf, StateSync> STREAM_CODEC =
                ByteBufCodecs.optional(TrackingState.STREAM_CODEC).map(StateSync::new, StateSync::state);

        @Override
        public Type<StateSync> type() {
            return TYPE;
        }
    }

    private static final StreamCodec<ByteBuf, Vec3> VEC3_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, position -> position.x,
            ByteBufCodecs.DOUBLE, position -> position.y,
            ByteBufCodecs.DOUBLE, position -> position.z,
            Vec3::new);
    private static final double TARGET_MOVED_RESEND_DISTANCE_SQUARED = 16;

    public record PassivePing(ResourceLocation creature, int level, TrackingDirection direction, Optional<TrackingHeight> height,
                              Optional<Integer> roundedDistance, Optional<Vec3> wispTarget) implements CustomPacketPayload {
        static final Type<PassivePing> TYPE = new Type<>(BeastsAndBountiesMod.id("passive_ping"));
        static final StreamCodec<RegistryFriendlyByteBuf, PassivePing> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, PassivePing::creature,
                ByteBufCodecs.VAR_INT, PassivePing::level,
                NeoForgeStreamCodecs.enumCodec(TrackingDirection.class), PassivePing::direction,
                ByteBufCodecs.optional(NeoForgeStreamCodecs.enumCodec(TrackingHeight.class)), PassivePing::height,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), PassivePing::roundedDistance,
                ByteBufCodecs.optional(VEC3_STREAM_CODEC), PassivePing::wispTarget,
                PassivePing::new);

        @Override
        public Type<PassivePing> type() {
            return TYPE;
        }
    }

    public record HuntTarget(int entityId, Vec3 position) {
        static final StreamCodec<ByteBuf, HuntTarget> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, HuntTarget::entityId,
                VEC3_STREAM_CODEC, HuntTarget::position,
                HuntTarget::new);

        boolean isCloseTo(HuntTarget other) {
            return entityId == other.entityId && position.distanceToSqr(other.position) < TARGET_MOVED_RESEND_DISTANCE_SQUARED;
        }
    }

    public record HuntUpdate(Optional<HuntTarget> target, List<Integer> glowIds) implements CustomPacketPayload {
        public static final HuntUpdate NONE = new HuntUpdate(Optional.empty(), List.of());
        static final Type<HuntUpdate> TYPE = new Type<>(BeastsAndBountiesMod.id("hunt_update"));
        static final StreamCodec<ByteBuf, HuntUpdate> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(HuntTarget.STREAM_CODEC), HuntUpdate::target,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), HuntUpdate::glowIds,
                HuntUpdate::new);

        boolean needsNoResendFor(HuntUpdate newer) {
            if (!glowIds.equals(newer.glowIds) || target.isPresent() != newer.target.isPresent()) return false;
            return target.isEmpty() || target.get().isCloseTo(newer.target.get());
        }

        @Override
        public Type<HuntUpdate> type() {
            return TYPE;
        }
    }
}
