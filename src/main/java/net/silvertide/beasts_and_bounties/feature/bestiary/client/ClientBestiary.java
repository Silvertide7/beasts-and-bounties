package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.Progress;

import java.util.HashMap;
import java.util.Map;

public final class ClientBestiary {
    private static final Component UNKNOWN_NAME = Component.literal("???");

    private static Map<ResourceLocation, BestiaryEntry> entries = Map.of();
    private static Map<ResourceLocation, Progress> progress = new HashMap<>();

    private ClientBestiary() {}

    public static void setEntries(Map<ResourceLocation, BestiaryEntry> newEntries) {
        entries = Map.copyOf(newEntries);
        refreshOpenScreen();
    }

    public static void setProgress(Map<ResourceLocation, Progress> newProgress) {
        progress = new HashMap<>(newProgress);
        refreshOpenScreen();
    }

    public static void updateProgress(ResourceLocation entityId, Progress newProgress) {
        Progress previous = progress(entityId);
        if (newProgress.equals(Progress.NONE)) progress.remove(entityId);
        else progress.put(entityId, newProgress);
        BestiaryEntry entry = entries.get(entityId);
        if (entry != null && entry.milestoneReached(newProgress) > entry.milestoneReached(previous))
            showMilestoneToast(entityId, entry, previous, newProgress);
        refreshOpenScreen();
    }

    static Map<ResourceLocation, BestiaryEntry> entries() {
        return entries;
    }

    static Progress progress(ResourceLocation entityId) {
        return progress.getOrDefault(entityId, Progress.NONE);
    }

    static Component displayName(ResourceLocation entityId, BestiaryEntry entry, Progress entityProgress) {
        if (!entry.knowledge(entityProgress).name()) return UNKNOWN_NAME;
        return BuiltInRegistries.ENTITY_TYPE.get(entityId).getDescription().copy().withStyle(entry.classification().nameColor());
    }

    private static void showMilestoneToast(ResourceLocation entityId, BestiaryEntry entry, Progress previous, Progress current) {
        Component title = Component.translatable(entry.isDiscovered(previous)
                ? "toast.beasts_and_bounties.updated"
                : "toast.beasts_and_bounties.discovered");
        Component name = entry.knowledge(current).name()
                ? BuiltInRegistries.ENTITY_TYPE.get(entityId).getDescription()
                : UNKNOWN_NAME;
        BestiaryToast.show(title, name);
    }

    private static void refreshOpenScreen() {
        if (Minecraft.getInstance().screen instanceof BestiaryScreen screen) screen.refresh();
    }
}
