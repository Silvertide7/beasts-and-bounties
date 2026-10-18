package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.feature.combat.CombatTracking;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class BountyEvents {
    private static final long KILL_CREDIT_WINDOW_TICKS = 20 * 60;

    private BountyEvents() {}

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new BountyDefinitions(event.getRegistryAccess()));
    }

    @SubscribeEvent
    public static void syncOnJoin(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) Network.sendIfConnected(event.getPlayer(), new BountyPayloads.StateSync(PlayerBounties.state(event.getPlayer())));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        BountyCommand.register(event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void progressBountiesOfParticipants(LivingDeathEvent event) {
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
