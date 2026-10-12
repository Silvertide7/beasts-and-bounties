package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

public record Knowledge(
        boolean name,
        boolean model,
        Optional<Component> type,
        Optional<Component> description,
        Map<String, Either<Double, Tier>> stats,
        List<Component> attackPatterns,
        List<Component> strengths,
        List<Component> weaknesses,
        List<Component> habitat,
        List<Component> notes
) {
    public static final Knowledge EMPTY = new Knowledge(false, false, Optional.empty(), Optional.empty(), Map.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private static final Codec<List<Component>> TEXT_LIST = ComponentSerialization.CODEC.listOf();

    public static final MapCodec<Knowledge> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("name", false).forGetter(Knowledge::name),
            Codec.BOOL.optionalFieldOf("model", false).forGetter(Knowledge::model),
            ComponentSerialization.CODEC.optionalFieldOf("type").forGetter(Knowledge::type),
            ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Knowledge::description),
            Codec.unboundedMap(Codec.STRING, Codec.either(Codec.DOUBLE, Tier.CODEC)).optionalFieldOf("stats", Map.of()).forGetter(Knowledge::stats),
            TEXT_LIST.optionalFieldOf("attack_patterns", List.of()).forGetter(Knowledge::attackPatterns),
            TEXT_LIST.optionalFieldOf("strengths", List.of()).forGetter(Knowledge::strengths),
            TEXT_LIST.optionalFieldOf("weaknesses", List.of()).forGetter(Knowledge::weaknesses),
            TEXT_LIST.optionalFieldOf("habitat", List.of()).forGetter(Knowledge::habitat),
            TEXT_LIST.optionalFieldOf("notes", List.of()).forGetter(Knowledge::notes)
    ).apply(instance, Knowledge::new));

    public Knowledge merge(Knowledge later) {
        Map<String, Either<Double, Tier>> mergedStats = new TreeMap<>(stats);
        mergedStats.putAll(later.stats);
        return new Knowledge(
                name || later.name,
                model || later.model,
                later.type.or(() -> type),
                later.description.or(() -> description),
                mergedStats,
                concat(attackPatterns, later.attackPatterns),
                concat(strengths, later.strengths),
                concat(weaknesses, later.weaknesses),
                concat(habitat, later.habitat),
                concat(notes, later.notes));
    }

    public boolean hasDetails() {
        return !stats.isEmpty() || !attackPatterns.isEmpty() || !strengths.isEmpty() || !weaknesses.isEmpty() || !habitat.isEmpty() || !notes.isEmpty();
    }

    private static List<Component> concat(List<Component> earlier, List<Component> later) {
        return Stream.concat(earlier.stream(), later.stream()).toList();
    }
}
