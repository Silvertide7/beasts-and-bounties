package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.feature.tracking.client.ClientTracking;

import java.util.List;
import java.util.Optional;

public final class TrackingPayloads {
    private static final double TARGET_MOVED_RESEND_DISTANCE_SQUARED = 16;

    private TrackingPayloads() {}

    static void register() {
        Network.toServer(SetTracked.class, SetTracked::encode, SetTracked::decode, (payload, player) ->
                payload.creature().ifPresentOrElse(creature -> PlayerTracking.track(player, creature), () -> PlayerTracking.stop(player)));
        Network.toServer(StartHunt.class, StartHunt::encode, StartHunt::decode, (payload, player) -> PlayerTracking.startHunt(player));
        Network.toClient(StateSync.class, StateSync::encode, StateSync::decode, payload -> ClientTracking.setState(payload.state()));
        Network.toClient(PassivePing.class, PassivePing::encode, PassivePing::decode, payload -> ClientTracking.receivePing(payload));
        Network.toClient(HuntUpdate.class, HuntUpdate::encode, HuntUpdate::decode, payload -> ClientTracking.receiveHunt(payload));
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 position) {
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public record SetTracked(Optional<ResourceLocation> creature) {
        void encode(FriendlyByteBuf buf) {
            buf.writeOptional(creature, FriendlyByteBuf::writeResourceLocation);
        }

        static SetTracked decode(FriendlyByteBuf buf) {
            return new SetTracked(buf.readOptional(FriendlyByteBuf::readResourceLocation));
        }
    }

    public record StartHunt() {
        void encode(FriendlyByteBuf buf) {}

        static StartHunt decode(FriendlyByteBuf buf) {
            return new StartHunt();
        }
    }

    public record StateSync(Optional<TrackingState> state) {
        void encode(FriendlyByteBuf buf) {
            buf.writeOptional(state, (out, value) -> value.write(out));
        }

        static StateSync decode(FriendlyByteBuf buf) {
            return new StateSync(buf.readOptional(TrackingState::read));
        }
    }

    public record PassivePing(ResourceLocation creature, int level, TrackingDirection direction, Optional<TrackingHeight> height,
                              Optional<Integer> roundedDistance, Optional<Vec3> wispTarget) {
        void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(creature);
            buf.writeVarInt(level);
            buf.writeEnum(direction);
            buf.writeOptional(height, FriendlyByteBuf::writeEnum);
            buf.writeOptional(roundedDistance, FriendlyByteBuf::writeVarInt);
            buf.writeOptional(wispTarget, TrackingPayloads::writeVec3);
        }

        static PassivePing decode(FriendlyByteBuf buf) {
            return new PassivePing(buf.readResourceLocation(), buf.readVarInt(), buf.readEnum(TrackingDirection.class),
                    buf.readOptional(in -> in.readEnum(TrackingHeight.class)), buf.readOptional(FriendlyByteBuf::readVarInt),
                    buf.readOptional(TrackingPayloads::readVec3));
        }
    }

    public record HuntTarget(int entityId, Vec3 position) {
        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            writeVec3(buf, position);
        }

        static HuntTarget read(FriendlyByteBuf buf) {
            return new HuntTarget(buf.readVarInt(), readVec3(buf));
        }

        boolean isCloseTo(HuntTarget other) {
            return entityId == other.entityId && position.distanceToSqr(other.position) < TARGET_MOVED_RESEND_DISTANCE_SQUARED;
        }
    }

    public record HuntUpdate(Optional<HuntTarget> target, List<Integer> glowIds) {
        public static final HuntUpdate NONE = new HuntUpdate(Optional.empty(), List.of());

        void encode(FriendlyByteBuf buf) {
            buf.writeOptional(target, (out, value) -> value.write(out));
            buf.writeCollection(glowIds, FriendlyByteBuf::writeVarInt);
        }

        static HuntUpdate decode(FriendlyByteBuf buf) {
            return new HuntUpdate(buf.readOptional(HuntTarget::read), buf.readList(FriendlyByteBuf::readVarInt));
        }

        boolean needsNoResendFor(HuntUpdate newer) {
            if (!glowIds.equals(newer.glowIds) || target.isPresent() != newer.target.isPresent()) return false;
            return target.isEmpty() || target.get().isCloseTo(newer.target.get());
        }
    }
}
