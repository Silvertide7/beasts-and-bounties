package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.core.PlayerData;
import net.silvertide.beasts_and_bounties.core.PlayerDataEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class BestiaryProgress {
    private static final Codec<Map<ResourceLocation, Progress>> PROGRESS_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, Progress.CODEC).xmap(HashMap::new, Function.identity());
    private static final PlayerData<Map<ResourceLocation, Progress>> PROGRESS = PlayerDataEvents.register(new PlayerData<>("progress", PROGRESS_CODEC));

    private BestiaryProgress() {}

    public static Map<ResourceLocation, Progress> all(Player player) {
        return PROGRESS.getOrCache(player, HashMap::new);
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
        Map<ResourceLocation, Progress> all = all(player);
        if (progress.equals(Progress.NONE)) all.remove(entityId);
        else all.put(entityId, progress);
        persist(player);
        Network.sendIfConnected(player, new BestiaryPayloads.ProgressUpdate(entityId, progress));
        if (entry.milestoneReached(previous) != entry.milestoneReached(progress)) BestiaryAttributes.reapply(player);
        BestiaryAdvancements.awardFor(player, entry, previous, progress);
    }

    public static void resetAll(ServerPlayer player) {
        all(player).clear();
        persist(player);
        Network.sendIfConnected(player, new BestiaryPayloads.AllProgress(Map.of()));
        BestiaryAttributes.reapply(player);
    }

    public static void persist(Player player) {
        Map<ResourceLocation, Progress> all = all(player);
        if (all.isEmpty()) PROGRESS.remove(player);
        else PROGRESS.set(player, all);
    }

    private static int saturatingAdd(int current, int amount) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, (long) current + amount));
    }
}
