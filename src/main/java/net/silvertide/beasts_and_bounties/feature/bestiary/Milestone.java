package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.silvertide.beasts_and_bounties.core.StrictCodecs;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public record Milestone(Progress requirement, Knowledge knowledge, List<Reward> rewards) {
    public static final Codec<Milestone> CODEC = RecordCodecBuilder.<Milestone>create(instance -> instance.group(
            Progress.MAP_CODEC.forGetter(Milestone::requirement),
            Knowledge.MAP_CODEC.forGetter(Milestone::knowledge),
            StrictCodecs.optionalField("rewards", Reward.CODEC.listOf(), List.of()).forGetter(Milestone::rewards)
    ).apply(instance, Milestone::new)).flatXmap(Milestone::requireKillsOrBounties, Milestone::requireKillsOrBounties);

    static final Comparator<Milestone> UNLOCK_ORDER = Comparator.comparingInt((Milestone milestone) -> milestone.requirement.kills())
            .thenComparingInt(milestone -> milestone.requirement.bounties());

    Milestone combine(Milestone later) {
        return new Milestone(requirement, knowledge.merge(later.knowledge), Stream.concat(rewards.stream(), later.rewards.stream()).toList());
    }

    private static DataResult<Milestone> requireKillsOrBounties(Milestone milestone) {
        return milestone.requirement.equals(Progress.NONE)
                ? DataResult.error(() -> "A milestone needs kills, bounties, or both")
                : DataResult.success(milestone);
    }
}
