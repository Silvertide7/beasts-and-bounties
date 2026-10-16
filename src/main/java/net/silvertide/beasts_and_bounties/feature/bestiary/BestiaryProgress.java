package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.silvertide.beasts_and_bounties.core.Payloads;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BestiaryProgress {
    private static final Codec<Map<ResourceLocation, Progress>> PROGRESS_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Progress.CODEC).xmap(HashMap::new, Function.identity());

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BeastsAndBountiesMod.MODID);

    private static final Supplier<AttachmentType<Map<ResourceLocation, Progress>>> PROGRESS =
            ATTACHMENT_TYPES.register("progress",
                    () -> AttachmentType.<Map<ResourceLocation, Progress>>builder(() -> new HashMap<>())
                            .serialize(PROGRESS_CODEC, progress -> !progress.isEmpty())
                            .copyOnDeath()
                            .build());

    private BestiaryProgress() {}

    static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static Map<ResourceLocation, Progress> all(Player player) {
        return player.getData(PROGRESS);
    }

    public static Progress of(Player player, ResourceLocation entityId) {
        return all(player).getOrDefault(entityId, Progress.NONE);
    }

    public static Progress of(Player player, EntityType<?> entityType) {
        return of(player, EntityType.getKey(entityType));
    }

    public static Bonuses bonuses(Player player, EntityType<?> entityType) {
        BestiaryEntry entry = BestiaryEntries.get(entityType);
        return entry == null ? Bonuses.NONE : entry.bonuses(of(player, entityType));
    }

    public static Bonuses bonuses(Player player, ResourceLocation entityId) {
        BestiaryEntry entry = BestiaryEntries.get(entityId);
        return entry == null ? Bonuses.NONE : entry.bonuses(of(player, entityId));
    }

    public static void addKills(ServerPlayer player, ResourceLocation entityId, BestiaryEntry entry, int amount) {
        Progress progress = of(player, entityId);
        set(player, entityId, entry, progress.withKills(saturatingAdd(progress.kills(), amount)));
    }

    public static void addBounties(ServerPlayer player, ResourceLocation entityId, BestiaryEntry entry, int amount) {
        Progress progress = of(player, entityId);
        set(player, entityId, entry, progress.withBounties(saturatingAdd(progress.bounties(), amount)));
    }

    public static void set(ServerPlayer player, ResourceLocation entityId, BestiaryEntry entry, Progress progress) {
        Progress previous = of(player, entityId);
        if (progress.equals(Progress.NONE)) all(player).remove(entityId);
        else all(player).put(entityId, progress);
        Payloads.sendIfConnected(player, new BestiaryPayloads.ProgressUpdate(entityId, progress));
        if (entry.milestoneReached(previous) != entry.milestoneReached(progress)) BestiaryAttributes.reapply(player);
    }

    public static void resetAll(ServerPlayer player) {
        all(player).clear();
        Payloads.sendIfConnected(player, new BestiaryPayloads.AllProgress(Map.of()));
        BestiaryAttributes.reapply(player);
    }

    private static int saturatingAdd(int current, int amount) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, (long) current + amount));
    }
}
