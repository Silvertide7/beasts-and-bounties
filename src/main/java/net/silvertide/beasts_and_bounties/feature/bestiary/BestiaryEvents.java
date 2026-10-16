package net.silvertide.beasts_and_bounties.feature.bestiary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.Payloads;
import net.silvertide.silverlib.feature.combat.CombatTracking;

import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class BestiaryEvents {
    private static final long KILL_CREDIT_WINDOW_TICKS = 20 * 60;

    private BestiaryEvents() {}

    @SubscribeEvent
    static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new BestiaryEntries(event.getRegistryAccess()));
    }

    @SubscribeEvent
    static void syncEntriesAndProgress(OnDatapackSyncEvent event) {
        BestiaryPayloads.Entries entries = new BestiaryPayloads.Entries(BestiaryEntries.all());
        event.getRelevantPlayers().forEach(player -> {
            Payloads.sendIfConnected(player, entries, new BestiaryPayloads.AllProgress(Map.copyOf(BestiaryProgress.all(player))));
            BestiaryAttributes.reapply(player);
        });
    }

    @SubscribeEvent
    static void reapplyAttributesOnRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BestiaryAttributes.reapply(player);
        if (!event.isEndConquered()) player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        BestiaryCommand.register(event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void creditKillToParticipants(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        BestiaryEntry entry = BestiaryEntries.get(victim.getType());
        if (entry == null) return;

        PlayerList playerList = victim.getServer().getPlayerList();
        for (UUID participantId : CombatTracking.participants(victim, KILL_CREDIT_WINDOW_TICKS)) {
            ServerPlayer participant = playerList.getPlayer(participantId);
            if (participant != null) BestiaryProgress.addKills(participant, EntityType.getKey(victim.getType()), entry, 1);
        }
    }

    @SubscribeEvent
    static void applyDamageMultipliers(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        LivingEntity victim = event.getEntity();
        double multiplier = 1;
        if (attacker instanceof ServerPlayer player) multiplier *= BestiaryProgress.bonuses(player, victim.getType()).damageMultiplier();
        if (victim instanceof ServerPlayer player) multiplier *= BestiaryProgress.bonuses(player, attacker.getType()).damageTakenMultiplier();
        if (multiplier != 1) event.setAmount((float) (event.getAmount() * multiplier));
    }

    @SubscribeEvent
    static void rollExtraLoot(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || victim.isBaby() || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        ServerPlayer killer = CombatTracking.attributedPlayer(event.getSource().getEntity());
        if (killer == null) return;
        double chance = BestiaryProgress.bonuses(killer, victim.getType()).extraLootChance();
        if (chance <= 0 || victim.getRandom().nextDouble() >= chance) return;

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, event.getSource())
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, event.getSource().getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, event.getSource().getDirectEntity())
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .withLuck(killer.getLuck())
                .create(LootContextParamSets.ENTITY);
        level.getServer().reloadableRegistries().getLootTable(victim.getLootTable()).getRandomItems(params, stack -> {
            ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack);
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        });
    }

    @SubscribeEvent
    static void applyBonusExperience(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player)) return;
        double multiplier = BestiaryProgress.bonuses(player, event.getEntity().getType()).experienceMultiplier();
        if (multiplier != 1) event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * multiplier));
    }
}
