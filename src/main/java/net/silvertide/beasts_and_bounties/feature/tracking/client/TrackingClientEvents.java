package net.silvertide.beasts_and_bounties.feature.tracking.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BeastsAndBountiesMod.MODID, value = Dist.CLIENT)
public final class TrackingClientEvents {
    static final KeyMapping HUNT_KEY = new KeyMapping("key.beasts_and_bounties.hunt",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.beasts_and_bounties");

    private TrackingClientEvents() {}

    @SubscribeEvent
    public static void tickClientTracking(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null) ClientTracking.tickHuntKey(HUNT_KEY.isDown() && minecraft.screen == null, level.getGameTime());
        WispTrail.tick();
    }

    public static Component huntKeyName() {
        return HUNT_KEY.getTranslatedKeyMessage();
    }

    @SubscribeEvent
    public static void forgetTrackingOnLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTracking.clear();
    }
}
