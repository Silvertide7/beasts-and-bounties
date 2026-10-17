package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record BountyDefinition(int kills, int weight, int bonusKills, List<BountyReward> rewards) {
    public static final Codec<BountyDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("kills", 3).forGetter(BountyDefinition::kills),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 10).forGetter(BountyDefinition::weight),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("bonus_kills", 0).forGetter(BountyDefinition::bonusKills),
            BountyReward.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(BountyDefinition::rewards)
    ).apply(instance, BountyDefinition::new));

    public List<ItemStack> itemRewards() {
        return rewards.stream().filter(BountyReward.Items.class::isInstance).map(reward -> ((BountyReward.Items) reward).stack()).toList();
    }

    public List<BountyReward.SkillExperience> skillExperienceRewards() {
        return rewards.stream().filter(BountyReward.SkillExperience.class::isInstance).map(BountyReward.SkillExperience.class::cast).toList();
    }

    public int experienceReward() {
        return rewards.stream().filter(BountyReward.Experience.class::isInstance).mapToInt(reward -> ((BountyReward.Experience) reward).amount()).sum();
    }
}
