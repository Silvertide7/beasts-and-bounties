package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public sealed interface Reward {
    Codec<Reward> CODEC = Type.CODEC.dispatch("type", Reward::type, Type::codec);

    Type type();

    record AttributeBonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) implements Reward {
        static final MapCodec<AttributeBonus> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Attribute.CODEC.fieldOf("attribute").forGetter(AttributeBonus::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttributeBonus::amount),
                AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(AttributeBonus::operation)
        ).apply(instance, AttributeBonus::new));

        @Override
        public Type type() {
            return Type.ATTRIBUTE;
        }
    }

    record DamageBonus(double amount) implements Reward {
        static final MapCodec<DamageBonus> CODEC = Codec.DOUBLE.fieldOf("amount").xmap(DamageBonus::new, DamageBonus::amount);

        @Override
        public Type type() {
            return Type.DAMAGE_BONUS;
        }
    }

    record DamageResistance(double amount) implements Reward {
        static final MapCodec<DamageResistance> CODEC = Codec.DOUBLE.fieldOf("amount").xmap(DamageResistance::new, DamageResistance::amount);

        @Override
        public Type type() {
            return Type.DAMAGE_RESISTANCE;
        }
    }

    record ExtraLoot(double chance) implements Reward {
        static final MapCodec<ExtraLoot> CODEC = Codec.doubleRange(0, 1).fieldOf("chance").xmap(ExtraLoot::new, ExtraLoot::chance);

        @Override
        public Type type() {
            return Type.EXTRA_LOOT;
        }
    }

    record BonusExperience(double amount) implements Reward {
        static final MapCodec<BonusExperience> CODEC = Codec.DOUBLE.fieldOf("amount").xmap(BonusExperience::new, BonusExperience::amount);

        @Override
        public Type type() {
            return Type.BONUS_EXPERIENCE;
        }
    }

    enum Type implements StringRepresentable {
        ATTRIBUTE("attribute"),
        DAMAGE_BONUS("damage_bonus"),
        DAMAGE_RESISTANCE("damage_resistance"),
        EXTRA_LOOT("extra_loot"),
        BONUS_EXPERIENCE("bonus_experience");

        static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String serializedName;

        Type(String serializedName) {
            this.serializedName = serializedName;
        }

        MapCodec<? extends Reward> codec() {
            return switch (this) {
                case ATTRIBUTE -> AttributeBonus.CODEC;
                case DAMAGE_BONUS -> DamageBonus.CODEC;
                case DAMAGE_RESISTANCE -> DamageResistance.CODEC;
                case EXTRA_LOOT -> ExtraLoot.CODEC;
                case BONUS_EXPERIENCE -> BonusExperience.CODEC;
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
