package net.silvertide.beasts_and_bounties.feature.bestiary;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

final class BestiaryAdvancements {
    private static final ResourceLocation FIRST_ENTRY = BeastsAndBountiesMod.id("root");
    private static final ResourceLocation ENTRY_COMPLETE = BeastsAndBountiesMod.id("entry_complete");
    private static final ResourceLocation BESTIARY_COMPLETE = BeastsAndBountiesMod.id("bestiary_complete");

    private BestiaryAdvancements() {}

    static void awardFor(ServerPlayer player, BestiaryEntry entry, Progress previous, Progress progress) {
        int before = entry.milestoneReached(previous);
        int after = entry.milestoneReached(progress);
        if (after <= before) return;
        award(player, FIRST_ENTRY);
        if (after != entry.milestones().size() - 1) return;
        award(player, ENTRY_COMPLETE);
        if (everyEntryComplete(player)) award(player, BESTIARY_COMPLETE);
    }

    static boolean everyEntryComplete(ServerPlayer player) {
        return BestiaryEntries.all().entrySet().stream()
                .allMatch(entry -> entry.getValue().milestoneReached(BestiaryProgress.of(player, entry.getKey())) == entry.getValue().milestones().size() - 1);
    }

    private static void award(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) return;
        advancement.getCriteria().keySet().forEach(criterion -> player.getAdvancements().award(advancement, criterion));
    }
}
