package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.ClientBestiary;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BestiaryPayloads {
    private BestiaryPayloads() {}

    static void register() {
        Network.toClient(Entries.class, Entries::encode, Entries::decode, payload -> ClientBestiary.receiveEntries(payload.entries(), payload.first()));
        Network.toClient(AllProgress.class, AllProgress::encode, AllProgress::decode, payload -> ClientBestiary.setProgress(payload.progress()));
        Network.toClient(ProgressUpdate.class, ProgressUpdate::encode, ProgressUpdate::decode, payload -> ClientBestiary.updateProgress(payload.entityId(), payload.progress()));
    }

    public record Entries(Map<ResourceLocation, BestiaryEntry> entries, boolean first) {
        private static final Codec<Map<ResourceLocation, BestiaryEntry>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, BestiaryEntry.CODEC);
        private static final int MAX_CHUNK_JSON_CHARS = 200_000;

        public static List<Entries> chunked(Map<ResourceLocation, BestiaryEntry> all) {
            List<Entries> chunks = new ArrayList<>();
            Map<ResourceLocation, BestiaryEntry> chunk = new HashMap<>();
            int chunkChars = 0;
            for (Map.Entry<ResourceLocation, BestiaryEntry> entry : all.entrySet()) {
                int entryChars = BestiaryEntry.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue()).result().map(json -> json.toString().length()).orElse(0);
                if (!chunk.isEmpty() && chunkChars + entryChars > MAX_CHUNK_JSON_CHARS) {
                    chunks.add(new Entries(Map.copyOf(chunk), chunks.isEmpty()));
                    chunk.clear();
                    chunkChars = 0;
                }
                chunk.put(entry.getKey(), entry.getValue());
                chunkChars += entryChars;
            }
            chunks.add(new Entries(Map.copyOf(chunk), chunks.isEmpty()));
            return chunks;
        }

        void encode(FriendlyByteBuf buf) {
            String json = CODEC.encodeStart(JsonOps.INSTANCE, entries).getOrThrow(false, error -> {}).toString();
            buf.writeByteArray(json.getBytes(StandardCharsets.UTF_8));
            buf.writeBoolean(first);
        }

        static Entries decode(FriendlyByteBuf buf) {
            String json = new String(buf.readByteArray(), StandardCharsets.UTF_8);
            Map<ResourceLocation, BestiaryEntry> entries = CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(false, error -> {});
            return new Entries(entries, buf.readBoolean());
        }
    }

    public record AllProgress(Map<ResourceLocation, Progress> progress) {
        void encode(FriendlyByteBuf buf) {
            buf.writeMap(progress, FriendlyByteBuf::writeResourceLocation, (out, value) -> value.write(out));
        }

        static AllProgress decode(FriendlyByteBuf buf) {
            return new AllProgress(buf.readMap(FriendlyByteBuf::readResourceLocation, Progress::read));
        }
    }

    public record ProgressUpdate(ResourceLocation entityId, Progress progress) {
        void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(entityId);
            progress.write(buf);
        }

        static ProgressUpdate decode(FriendlyByteBuf buf) {
            return new ProgressUpdate(buf.readResourceLocation(), Progress.read(buf));
        }
    }
}
