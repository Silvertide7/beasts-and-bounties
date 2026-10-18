package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

public sealed interface Reward {
    Codec<Reward> CODEC = Type.CODEC.dispatch("type", Reward::type, Type::codec);

    Type type();

    Codec<AttributeModifier.Operation> OPERATION_CODEC = Codec.STRING.comapFlatMap(Reward::parseOperation, Reward::operationName);

    record AttributeBonus(Attribute attribute, double amount, AttributeModifier.Operation operation) implements Reward {
        static final MapCodec<AttributeBonus> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ForgeRegistries.ATTRIBUTES.getCodec().fieldOf("attribute").forGetter(AttributeBonus::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttributeBonus::amount),
                OPERATION_CODEC.fieldOf("operation").forGetter(AttributeBonus::operation)
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

    record PassiveTracking(int level) implements Reward {
        static final MapCodec<PassiveTracking> CODEC = Codec.intRange(1, 3).fieldOf("level").xmap(PassiveTracking::new, PassiveTracking::level);

        @Override
        public Type type() {
            return Type.PASSIVE_TRACKING;
        }
    }

    record ActiveTracking(int level) implements Reward {
        static final MapCodec<ActiveTracking> CODEC = Codec.intRange(1, 3).fieldOf("level").xmap(ActiveTracking::new, ActiveTracking::level);

        @Override
        public Type type() {
            return Type.ACTIVE_TRACKING;
        }
    }

    static String operationName(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADDITION -> "add_value";
            case MULTIPLY_BASE -> "add_multiplied_base";
            case MULTIPLY_TOTAL -> "add_multiplied_total";
        };
    }

    static DataResult<AttributeModifier.Operation> parseOperation(String name) {
        return Arrays.stream(AttributeModifier.Operation.values())
                .filter(operation -> operationName(operation).equals(name) || operation.name().equalsIgnoreCase(name))
                .findFirst()
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Unknown attribute operation: " + name));
    }

    enum Type implements StringRepresentable {
        ATTRIBUTE("attribute"),
        DAMAGE_BONUS("damage_bonus"),
        DAMAGE_RESISTANCE("damage_resistance"),
        EXTRA_LOOT("extra_loot"),
        BONUS_EXPERIENCE("bonus_experience"),
        PASSIVE_TRACKING("passive_tracking"),
        ACTIVE_TRACKING("active_tracking");

        static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String serializedName;

        Type(String serializedName) {
            this.serializedName = serializedName;
        }

        Codec<? extends Reward> codec() {
            return switch (this) {
                case ATTRIBUTE -> AttributeBonus.CODEC.codec();
                case DAMAGE_BONUS -> DamageBonus.CODEC.codec();
                case DAMAGE_RESISTANCE -> DamageResistance.CODEC.codec();
                case EXTRA_LOOT -> ExtraLoot.CODEC.codec();
                case BONUS_EXPERIENCE -> BonusExperience.CODEC.codec();
                case PASSIVE_TRACKING -> PassiveTracking.CODEC.codec();
                case ACTIVE_TRACKING -> ActiveTracking.CODEC.codec();
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
