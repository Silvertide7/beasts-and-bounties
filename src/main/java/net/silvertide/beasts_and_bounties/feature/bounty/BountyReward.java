package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public sealed interface BountyReward {
    Codec<BountyReward> CODEC = Type.CODEC.dispatch("type", BountyReward::type, Type::codec);

    Type type();

    record Items(Item item, int count) implements BountyReward {
        static final MapCodec<Items> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Items::item),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(Items::count)
        ).apply(instance, Items::new));

        public ItemStack stack() {
            return new ItemStack(item, count);
        }

        @Override
        public Type type() {
            return Type.ITEM;
        }
    }

    record Experience(int amount) implements BountyReward {
        static final MapCodec<Experience> CODEC = ExtraCodecs.POSITIVE_INT.fieldOf("amount").xmap(Experience::new, Experience::amount);

        @Override
        public Type type() {
            return Type.EXPERIENCE;
        }
    }

    enum Type implements StringRepresentable {
        ITEM("item"),
        EXPERIENCE("experience");

        static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String serializedName;

        Type(String serializedName) {
            this.serializedName = serializedName;
        }

        MapCodec<? extends BountyReward> codec() {
            return switch (this) {
                case ITEM -> Items.CODEC;
                case EXPERIENCE -> Experience.CODEC;
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
