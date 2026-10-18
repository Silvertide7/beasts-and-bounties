package net.silvertide.beasts_and_bounties.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Optional;
import java.util.stream.Stream;

public final class StrictCodecs {
    private StrictCodecs() {}

    public static <A> MapCodec<Optional<A>> optionalField(String name, Codec<A> elementCodec) {
        return new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString(name));
            }

            @Override
            public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get(name);
                if (value == null) return DataResult.success(Optional.empty());
                return elementCodec.parse(ops, value).map(Optional::of);
            }

            @Override
            public <T> RecordBuilder<T> encode(Optional<A> value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return value.map(present -> prefix.add(name, elementCodec.encodeStart(ops, present))).orElse(prefix);
            }
        };
    }

    public static <A> MapCodec<A> optionalField(String name, Codec<A> elementCodec, A fallback) {
        return optionalField(name, elementCodec).xmap(value -> value.orElse(fallback), value -> Optional.of(value).filter(present -> !present.equals(fallback)));
    }
}
