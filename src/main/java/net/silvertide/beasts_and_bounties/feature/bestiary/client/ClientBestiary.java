package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.Bonuses;
import net.silvertide.beasts_and_bounties.feature.bestiary.Progress;

import java.util.HashMap;
import java.util.Map;

public final class ClientBestiary {
    private static Map<ResourceLocation, BestiaryEntry> entries = Map.of();
    private static Map<ResourceLocation, Progress> progress = new HashMap<>();

    private ClientBestiary() {}

    public static void receiveEntries(Map<ResourceLocation, BestiaryEntry> chunk, boolean first) {
        Map<ResourceLocation, BestiaryEntry> merged = first ? new HashMap<>() : new HashMap<>(entries);
        merged.putAll(chunk);
        entries = Map.copyOf(merged);
        BestiaryScreen.refreshIfOpen();
    }

    public static void setProgress(Map<ResourceLocation, Progress> newProgress) {
        progress = new HashMap<>(newProgress);
        BestiaryScreen.refreshIfOpen();
    }

    public static void updateProgress(ResourceLocation entityId, Progress newProgress) {
        Progress previous = progress(entityId);
        if (newProgress.equals(Progress.NONE)) progress.remove(entityId);
        else progress.put(entityId, newProgress);
        BestiaryEntry entry = entries.get(entityId);
        if (entry != null && entry.milestoneReached(newProgress) > entry.milestoneReached(previous))
            showMilestoneToast(entityId, entry, previous, newProgress);
        BestiaryScreen.refreshIfOpen();
    }

    static Map<ResourceLocation, BestiaryEntry> entries() {
        return entries;
    }

    static Progress progress(ResourceLocation entityId) {
        return progress.getOrDefault(entityId, Progress.NONE);
    }

    public static Bonuses bonuses(ResourceLocation entityId) {
        BestiaryEntry entry = entries.get(entityId);
        return entry == null ? Bonuses.NONE : entry.bonuses(progress(entityId));
    }

    public static Component creatureName(ResourceLocation entityId) {
        BestiaryEntry entry = entries.get(entityId);
        return entry == null ? ForgeRegistries.ENTITY_TYPES.getValue(entityId).getDescription() : entry.displayName(entityId, progress(entityId));
    }

    static Component displayName(ResourceLocation entityId, BestiaryEntry entry, Progress entityProgress) {
        Component name = entry.displayName(entityId, entityProgress);
        return entry.knowledge(entityProgress).name() ? name.copy().withStyle(entry.classification().nameColor()) : name;
    }

    private static void showMilestoneToast(ResourceLocation entityId, BestiaryEntry entry, Progress previous, Progress current) {
        Component title = Component.translatable(entry.isDiscovered(previous)
                ? "toast.beasts_and_bounties.updated"
                : "toast.beasts_and_bounties.discovered");
        BestiaryToast.show(title, entry.displayName(entityId, current));
    }
}
