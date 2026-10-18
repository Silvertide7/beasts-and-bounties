package net.silvertide.beasts_and_bounties.core;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class PlayerDataEvents {
    private static final List<PlayerData<?>> ALL = new ArrayList<>();

    private PlayerDataEvents() {}

    public static <T> PlayerData<T> register(PlayerData<T> data) {
        ALL.add(data);
        return data;
    }

    @SubscribeEvent
    public static void forgetOnLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        ALL.forEach(data -> data.forget(playerId));
    }
}
