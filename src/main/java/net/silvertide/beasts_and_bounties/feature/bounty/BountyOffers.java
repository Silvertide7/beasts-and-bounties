package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BountyOffers {
    public record Offer(ResourceLocation entityId, BountyDefinition definition) {}

    private BountyOffers() {}

    public static List<Offer> forPlayer(ServerPlayer player, long now) {
        return pick(player.getUUID(), player.serverLevel().getSeed(), now / BountyConfig.refreshTicks(), PlayerBounties.state(player).rerolls(),
                BountyConfig.OFFERS_SHOWN.get(), BountyDefinitions.all());
    }

    public static long periodEndsAt(long now) {
        long refresh = BountyConfig.refreshTicks();
        return (now / refresh + 1) * refresh;
    }

    static List<Offer> pick(UUID playerId, long worldSeed, long period, int rerolls, int count, Map<ResourceLocation, BountyDefinition> definitions) {
        List<WeightedEntry.Wrapper<Offer>> candidates = new ArrayList<>();
        definitions.forEach((entityId, definition) -> {
            if (BestiaryEntries.get(entityId) != null) candidates.add(WeightedEntry.wrap(new Offer(entityId, definition), definition.weight()));
        });
        RandomSource random = RandomSource.create(playerId.getMostSignificantBits() * 31 + playerId.getLeastSignificantBits()
                ^ period * 0x9E3779B97F4A7C15L ^ worldSeed ^ rerolls * 0xC2B2AE3D27D4EB4FL);
        List<Offer> offers = new ArrayList<>();
        while (offers.size() < count && !candidates.isEmpty()) {
            WeightedEntry.Wrapper<Offer> chosen = WeightedRandom.getRandomItem(random, candidates).orElseThrow();
            candidates.remove(chosen);
            offers.add(chosen.data());
        }
        return offers;
    }
}
