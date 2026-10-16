package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.Payloads;
import net.silvertide.silverlib.feature.combat.CombatTracking;

import java.util.UUID;

@EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class BountyEvents {
    private static final long KILL_CREDIT_WINDOW_TICKS = 20 * 60;

    private BountyEvents() {}

    @SubscribeEvent
    static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new BountyDefinitions(event.getRegistryAccess()));
    }

    @SubscribeEvent
    static void syncOnJoin(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) Payloads.sendIfConnected(event.getPlayer(), new BountyPayloads.StateSync(PlayerBounties.state(event.getPlayer())));
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        BountyCommand.register(event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void progressBountiesOfParticipants(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        ResourceLocation entityId = EntityType.getKey(victim.getType());
        PlayerList playerList = victim.getServer().getPlayerList();
        for (UUID participantId : CombatTracking.participants(victim, KILL_CREDIT_WINDOW_TICKS)) {
            ServerPlayer participant = playerList.getPlayer(participantId);
            if (participant != null) PlayerBounties.recordKill(participant, entityId);
        }
    }
}
