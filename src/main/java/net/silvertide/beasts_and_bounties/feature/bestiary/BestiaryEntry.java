package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class BestiaryEntry {
    public static final Codec<BestiaryEntry> CODEC = RecordCodecBuilder.<BestiaryEntry>create(instance -> instance.group(
            Classification.CODEC.optionalFieldOf("classification", Classification.NORMAL).forGetter(BestiaryEntry::classification),
            Codec.list(Milestone.CODEC, 1, Integer.MAX_VALUE).fieldOf("milestones").forGetter(BestiaryEntry::milestones)
    ).apply(instance, BestiaryEntry::new));

    private static final int NO_MILESTONE = -1;
    private static final Component UNKNOWN_NAME = Component.literal("???");

    private final Classification classification;
    private final List<Milestone> milestones;
    private final Knowledge[] knowledgeAtMilestone;
    private final Bonuses[] bonusesAtMilestone;

    public BestiaryEntry(Classification classification, List<Milestone> milestones) {
        this.classification = classification;
        this.milestones = combineSameRequirements(milestones);
        int count = this.milestones.size();
        knowledgeAtMilestone = new Knowledge[count];
        bonusesAtMilestone = new Bonuses[count];

        Knowledge knowledge = Knowledge.EMPTY;
        double damageBonus = 0;
        double damageResistance = 0;
        double extraLootChance = 0;
        double bonusExperience = 0;
        int passiveTracking = 0;
        int activeTracking = 0;
        for (int i = 0; i < count; i++) {
            Milestone milestone = this.milestones.get(i);
            knowledge = knowledge.merge(milestone.knowledge());
            for (Reward reward : milestone.rewards()) {
                switch (reward) {
                    case Reward.DamageBonus bonus -> damageBonus += bonus.amount();
                    case Reward.DamageResistance resistance -> damageResistance += resistance.amount();
                    case Reward.ExtraLoot extraLoot -> extraLootChance += extraLoot.chance();
                    case Reward.BonusExperience experience -> bonusExperience += experience.amount();
                    case Reward.PassiveTracking tracking -> passiveTracking = Math.max(passiveTracking, tracking.level());
                    case Reward.ActiveTracking tracking -> activeTracking = Math.max(activeTracking, tracking.level());
                    case Reward.AttributeBonus attribute -> {}
                }
            }
            knowledgeAtMilestone[i] = knowledge;
            bonusesAtMilestone[i] = Bonuses.fromTotals(damageBonus, damageResistance, extraLootChance, bonusExperience, passiveTracking, activeTracking);
        }
    }

    public Classification classification() {
        return classification;
    }

    public List<Milestone> milestones() {
        return milestones;
    }

    public int milestoneReached(Progress progress) {
        int reached = NO_MILESTONE;
        while (reached + 1 < milestones.size() && progress.meets(milestones.get(reached + 1).requirement())) reached++;
        return reached;
    }

    public boolean isDiscovered(Progress progress) {
        return milestoneReached(progress) != NO_MILESTONE;
    }

    public Knowledge knowledge(Progress progress) {
        int reached = milestoneReached(progress);
        return reached == NO_MILESTONE ? Knowledge.EMPTY : knowledgeAtMilestone[reached];
    }

    public Component displayName(ResourceLocation entityId, Progress progress) {
        return knowledge(progress).name() ? BuiltInRegistries.ENTITY_TYPE.get(entityId).getDescription() : UNKNOWN_NAME;
    }

    public Knowledge fullKnowledge() {
        return knowledgeAtMilestone[knowledgeAtMilestone.length - 1];
    }

    public Bonuses bonuses(Progress progress) {
        int reached = milestoneReached(progress);
        return reached == NO_MILESTONE ? Bonuses.NONE : bonusesAtMilestone[reached];
    }

    public Optional<Progress> nextRequirement(Progress progress) {
        int next = milestoneReached(progress) + 1;
        return next < milestones.size() ? Optional.of(milestones.get(next).requirement()) : Optional.empty();
    }

    public boolean usesBounties() {
        return milestones.stream().anyMatch(milestone -> milestone.requirement().bounties() > 0);
    }

    public Stream<Reward> rewardsEarned(Progress progress) {
        return milestones.subList(0, milestoneReached(progress) + 1).stream().flatMap(milestone -> milestone.rewards().stream());
    }

    private static List<Milestone> combineSameRequirements(List<Milestone> milestones) {
        List<Milestone> combined = new ArrayList<>();
        for (Milestone milestone : milestones.stream().sorted(Milestone.UNLOCK_ORDER).toList()) {
            int last = combined.size() - 1;
            if (last >= 0 && combined.get(last).requirement().equals(milestone.requirement())) combined.set(last, combined.get(last).combine(milestone));
            else combined.add(milestone);
        }
        return List.copyOf(combined);
    }
}
