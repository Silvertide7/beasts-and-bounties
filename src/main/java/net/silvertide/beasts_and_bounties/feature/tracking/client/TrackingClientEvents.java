package net.silvertide.beasts_and_bounties.feature.tracking.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingParticles;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BeastsAndBountiesMod.MODID, value = Dist.CLIENT)
public final class TrackingClientEvents {
    private static final KeyMapping HUNT_KEY = new KeyMapping("key.beasts_and_bounties.hunt",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.beasts_and_bounties");

    private TrackingClientEvents() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(HUNT_KEY);
    }

    @SubscribeEvent
    static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, BeastsAndBountiesMod.id("tracking"), TrackingHud::render);
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(TrackingParticles.TRACKING_WISP.get(), WispParticle.Provider::new);
    }

    @SubscribeEvent
    static void tickClientTracking(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != null) ClientTracking.tickHuntKey(HUNT_KEY.isDown() && minecraft.screen == null, level.getGameTime());
        WispTrail.tick();
    }

    public static Component huntKeyName() {
        return HUNT_KEY.getTranslatedKeyMessage();
    }

    @SubscribeEvent
    static void forgetTrackingOnLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTracking.clear();
    }
}
