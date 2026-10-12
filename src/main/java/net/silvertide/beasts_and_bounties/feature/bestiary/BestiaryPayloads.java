package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.ClientBestiary;

import java.util.HashMap;
import java.util.Map;

public final class BestiaryPayloads {
    private BestiaryPayloads() {}

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(Entries.TYPE, Entries.STREAM_CODEC, (payload, context) -> ClientBestiary.setEntries(payload.entries()));
        registrar.playToClient(AllProgress.TYPE, AllProgress.STREAM_CODEC, (payload, context) -> ClientBestiary.setProgress(payload.progress()));
        registrar.playToClient(ProgressUpdate.TYPE, ProgressUpdate.STREAM_CODEC, (payload, context) -> ClientBestiary.updateProgress(payload.entityId(), payload.progress()));
    }

    public record Entries(Map<ResourceLocation, BestiaryEntry> entries) implements CustomPacketPayload {
        static final Type<Entries> TYPE = new Type<>(BeastsAndBountiesMod.id("entries"));
        static final StreamCodec<RegistryFriendlyByteBuf, Entries> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(Codec.unboundedMap(ResourceLocation.CODEC, BestiaryEntry.CODEC)).map(Entries::new, Entries::entries);

        @Override
        public Type<Entries> type() {
            return TYPE;
        }
    }

    public record AllProgress(Map<ResourceLocation, Progress> progress) implements CustomPacketPayload {
        static final Type<AllProgress> TYPE = new Type<>(BeastsAndBountiesMod.id("all_progress"));
        static final StreamCodec<RegistryFriendlyByteBuf, AllProgress> STREAM_CODEC =
                ByteBufCodecs.<RegistryFriendlyByteBuf, ResourceLocation, Progress, Map<ResourceLocation, Progress>>map(HashMap::new, ResourceLocation.STREAM_CODEC, Progress.STREAM_CODEC)
                        .map(AllProgress::new, AllProgress::progress);

        @Override
        public Type<AllProgress> type() {
            return TYPE;
        }
    }

    public record ProgressUpdate(ResourceLocation entityId, Progress progress) implements CustomPacketPayload {
        static final Type<ProgressUpdate> TYPE = new Type<>(BeastsAndBountiesMod.id("progress_update"));
        static final StreamCodec<RegistryFriendlyByteBuf, ProgressUpdate> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, ProgressUpdate::entityId,
                Progress.STREAM_CODEC, ProgressUpdate::progress,
                ProgressUpdate::new);

        @Override
        public Type<ProgressUpdate> type() {
            return TYPE;
        }
    }
}
