package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        List<Offer> picked = pick(server.overworld().getSeed(), period, board.rerolls(), BountyConfig.OFFERS_SHOWN.get(), definitions, Set.copyOf(board.offers()));
        board.record(period, picked.stream().map(Offer::entityId).toList());
        return picked;
    }

    public static long periodEndsAt(long now) {
        long refresh = BountyConfig.refreshTicks();
        return (now / refresh + 1) * refresh;
    }

    public static List<Offer> pick(long worldSeed, long period, int rerolls, int count, Map<ResourceLocation, BountyDefinition> definitions, Set<ResourceLocation> previousOffers) {
        List<WeightedEntry.Wrapper<Offer>> candidates = new ArrayList<>();
        definitions.forEach((entityId, definition) -> {
            if (BestiaryEntries.get(entityId) != null) candidates.add(WeightedEntry.wrap(new Offer(entityId, definition), definition.weight()));
        });
        if (candidates.size() - previousOffers.size() >= count) candidates.removeIf(candidate -> previousOffers.contains(candidate.getData().entityId()));
        RandomSource random = RandomSource.create(period * 0x9E3779B97F4A7C15L ^ worldSeed ^ rerolls * 0xC2B2AE3D27D4EB4FL);
        List<Offer> offers = new ArrayList<>();
        while (offers.size() < count && !candidates.isEmpty()) {
            WeightedEntry.Wrapper<Offer> chosen = WeightedRandom.getRandomItem(random, candidates).orElseThrow();
            candidates.remove(chosen);
            offers.add(chosen.getData());
        }
        return offers;
    }
}
