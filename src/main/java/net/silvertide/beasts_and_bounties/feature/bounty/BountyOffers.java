package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.Classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class BountyOffers {
    public record Offer(ResourceLocation entityId, BountyDefinition definition) {}

    private BountyOffers() {}

    public static List<Offer> current(MinecraftServer server, long now) {
        BountyBoardData board = BountyBoardData.of(server);
        long period = now / BountyConfig.refreshTicks();
        Map<ResourceLocation, BountyDefinition> definitions = BountyDefinitions.all();
        if (board.isCurrent(period)) {
            return board.offers().stream().filter(definitions::containsKey).map(id -> new Offer(id, definitions.get(id))).toList();
        }
        List<Offer> picked = pick(server.overworld().getSeed(), period, board.rerolls(), BountyConfig.OFFERS_SHOWN.get(), BountyConfig.BALANCED_POSTINGS.get(), definitions, Set.copyOf(board.offers()));
        board.record(period, picked.stream().map(Offer::entityId).toList());
        return picked;
    }

    public static long periodEndsAt(long now) {
        long refresh = BountyConfig.refreshTicks();
        return (now / refresh + 1) * refresh;
    }

    public static List<Offer> pick(long worldSeed, long period, int rerolls, int count, boolean balanced, Map<ResourceLocation, BountyDefinition> definitions, Set<ResourceLocation> previousOffers) {
        List<WeightedEntry.Wrapper<Offer>> candidates = new ArrayList<>();
        definitions.forEach((entityId, definition) -> {
            if (BestiaryEntries.get(entityId) != null) candidates.add(WeightedEntry.wrap(new Offer(entityId, definition), definition.weight()));
        });
        if (candidates.size() - previousOffers.size() >= count) candidates.removeIf(candidate -> previousOffers.contains(candidate.getData().entityId()));
        RandomSource random = RandomSource.create(period * 0x9E3779B97F4A7C15L ^ worldSeed ^ rerolls * 0xC2B2AE3D27D4EB4FL);
        List<Offer> offers = new ArrayList<>();
        if (balanced && count >= 2) {
            draw(random, candidates, offer -> !isNormal(offer)).ifPresent(offers::add);
            draw(random, candidates, BountyOffers::isNormal).ifPresent(offers::add);
        }
        while (offers.size() < count && !candidates.isEmpty()) offers.add(draw(random, candidates, offer -> true).orElseThrow());
        return offers;
    }

    private static Optional<Offer> draw(RandomSource random, List<WeightedEntry.Wrapper<Offer>> candidates, Predicate<Offer> filter) {
        List<WeightedEntry.Wrapper<Offer>> eligible = candidates.stream().filter(candidate -> filter.test(candidate.getData())).collect(Collectors.toList());
        Optional<WeightedEntry.Wrapper<Offer>> chosen = WeightedRandom.getRandomItem(random, eligible);
        chosen.ifPresent(candidates::remove);
        return chosen.map(WeightedEntry.Wrapper::getData);
    }

    private static boolean isNormal(Offer offer) {
        BestiaryEntry entry = BestiaryEntries.get(offer.entityId());
        return entry == null || entry.classification() == Classification.NORMAL;
    }
}
