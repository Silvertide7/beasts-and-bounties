package net.silvertide.beasts_and_bounties.feature.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public sealed interface BountyReward {
    Codec<BountyReward> CODEC = Type.CODEC.dispatch("type", BountyReward::type, Type::codec);

    Type type();

    record Items(Item item, int count) implements BountyReward {
        static final MapCodec<Items> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(Items::item),
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

    record SkillExperience(String skill, long amount) implements BountyReward {
        private static final String SEPARATOR = "/";
        static final MapCodec<SkillExperience> CODEC = Codec.STRING.comapFlatMap(SkillExperience::parse, SkillExperience::serialized).fieldOf("xp");

        private static DataResult<SkillExperience> parse(String text) {
            int separator = text.lastIndexOf(SEPARATOR);
            if (separator <= 0) return DataResult.error(() -> "expected skill/amount, got " + text);
            try {
                long amount = Long.parseLong(text.substring(separator + 1));
                if (amount <= 0) return DataResult.error(() -> "amount must be positive: " + text);
                return DataResult.success(new SkillExperience(text.substring(0, separator), amount));
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "amount is not a whole number: " + text);
            }
        }

        private String serialized() {
            return skill + SEPARATOR + amount;
        }

        @Override
        public Type type() {
            return Type.PMMO_XP;
        }
    }

    enum Type implements StringRepresentable {
        ITEM("item"),
        EXPERIENCE("experience"),
        PMMO_XP("pmmo_xp");

        static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String serializedName;

        Type(String serializedName) {
            this.serializedName = serializedName;
        }

        Codec<? extends BountyReward> codec() {
            return switch (this) {
                case ITEM -> Items.CODEC.codec();
                case EXPERIENCE -> Experience.CODEC.codec();
                case PMMO_XP -> SkillExperience.CODEC.codec();
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
