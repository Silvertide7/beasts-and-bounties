package net.silvertide.beasts_and_bounties.feature.bestiary;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.HashMap;
import java.util.Map;

final class BestiaryAttributes {
    private record AttributeOperation(Holder<Attribute> attribute, AttributeModifier.Operation operation) {}

    private BestiaryAttributes() {}

    static void reapply(ServerPlayer player) {
        removeAll(player);
        totalAttributeBonuses(player).forEach((key, amount) -> {
            AttributeInstance instance = player.getAttribute(key.attribute());
            if (instance != null) instance.addTransientModifier(new AttributeModifier(modifierId(key.operation()), amount, key.operation()));
        });
    }

    private static Map<AttributeOperation, Double> totalAttributeBonuses(ServerPlayer player) {
        Map<AttributeOperation, Double> totals = new HashMap<>();
        BestiaryProgress.all(player).forEach((entityId, progress) -> {
            BestiaryEntry entry = BestiaryEntries.get(entityId);
            if (entry == null) return;
            entry.rewardsEarned(progress).forEach(reward -> {
                if (reward instanceof Reward.AttributeBonus bonus)
                    totals.merge(new AttributeOperation(bonus.attribute(), bonus.operation()), bonus.amount(), Double::sum);
            });
        });
        return totals;
    }

    private static void removeAll(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;
            for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) instance.removeModifier(modifierId(operation));
        });
    }

    private static ResourceLocation modifierId(AttributeModifier.Operation operation) {
        return BeastsAndBountiesMod.id("bestiary_" + operation.getSerializedName());
    }
}
