package net.silvertide.beasts_and_bounties.core;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Payloads {
    private Payloads() {}

    public static void sendIfConnected(ServerPlayer player, CustomPacketPayload... payloads) {
        for (CustomPacketPayload payload : payloads)
            if (player.connection.hasChannel(payload.type())) PacketDistributor.sendToPlayer(player, payload);
    }
}
