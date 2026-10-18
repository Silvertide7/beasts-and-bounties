package net.silvertide.beasts_and_bounties.feature.bestiary;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BestiaryAttributes {
    private static final Map<AttributeModifier.Operation, UUID> MODIFIER_IDS = new EnumMap<>(AttributeModifier.Operation.class);

    static {
        for (AttributeModifier.Operation operation : AttributeModifier.Operation.values())
            MODIFIER_IDS.put(operation, UUID.nameUUIDFromBytes(modifierName(operation).getBytes(StandardCharsets.UTF_8)));
    }

    private record AttributeOperation(Attribute attribute, AttributeModifier.Operation operation) {}

    private BestiaryAttributes() {}

    public static void reapply(ServerPlayer player) {
        removeAll(player);
        totalAttributeBonuses(player).forEach((key, amount) -> {
            AttributeInstance instance = player.getAttribute(key.attribute());
            if (instance != null) instance.addTransientModifier(new AttributeModifier(MODIFIER_IDS.get(key.operation()), modifierName(key.operation()), amount, key.operation()));
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
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            for (UUID modifierId : MODIFIER_IDS.values()) instance.removeModifier(modifierId);
        }
    }

    private static String modifierName(AttributeModifier.Operation operation) {
        return BeastsAndBountiesMod.MODID + ":bestiary_" + operation.name().toLowerCase();
    }
}
