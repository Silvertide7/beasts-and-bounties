package net.silvertide.beasts_and_bounties.feature.tracking.client;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.BestiaryScreen;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.ClientBestiary;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingConfig;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingPayloads;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingState;

import java.util.List;
import java.util.Optional;

public final class ClientTracking {
    public static final int GLOW_COLOR = 0xFF5A5A;

    private static Optional<TrackingState> state = Optional.empty();
    private static TrackingPayloads.HuntUpdate hunt = TrackingPayloads.HuntUpdate.NONE;
    private static IntSet glowIds = IntSets.EMPTY_SET;
    private static int holdTicks;
    private static boolean holdSpent;
    private static List<FormattedCharSequence> pingLines = List.of();
    private static long pingShownAtMillis;

    private ClientTracking() {}

    public static void setState(Optional<TrackingState> newState) {
        Optional<ResourceLocation> previousCreature = trackedCreature();
        state = newState;
        if (!trackedCreature().equals(previousCreature)) forgetPing();
        BestiaryScreen.refreshIfOpen();
    }

    public static void receivePing(TrackingPayloads.PassivePing ping) {
        Minecraft minecraft = Minecraft.getInstance();
        int wrapWidth = Math.min(TrackingHud.PING_MAX_WIDTH, minecraft.getWindow().getGuiScaledWidth() - 2 * TrackingHud.PING_MARGIN);
        pingLines = minecraft.font.split(TrackingHud.pingText(ping), wrapWidth);
        pingShownAtMillis = Util.getMillis();
        ping.wispTarget().ifPresent(WispTrail::launchTowards);
    }

    public static void receiveHunt(TrackingPayloads.HuntUpdate update) {
        hunt = update;
        glowIds = update.glowIds().isEmpty() ? IntSets.EMPTY_SET : new IntOpenHashSet(update.glowIds());
    }

    public static void clear() {
        state = Optional.empty();
        forgetPing();
        receiveHunt(TrackingPayloads.HuntUpdate.NONE);
        holdTicks = 0;
        holdSpent = false;
    }

    public static boolean isGlowing(Entity entity) {
        return !glowIds.isEmpty() && glowIds.contains(entity.getId()) && isHunting(entity.level().getGameTime());
    }

    public static Optional<ResourceLocation> trackedCreature() {
        return state.flatMap(TrackingState::creature);
    }

    public static void requestTracking(Optional<ResourceLocation> creature) {
        PacketDistributor.sendToServer(new TrackingPayloads.SetTracked(creature));
    }

    static void tickHuntKey(boolean keyDown, long gameTime) {
        if (!keyDown) {
            holdTicks = 0;
            holdSpent = false;
            return;
        }
        if (holdSpent) return;
        if (holdTicks == 0 && !explainWhyHuntCannotStart(gameTime)) {
            holdSpent = true;
            return;
        }
        holdTicks++;
        if (holdTicks >= holdTicksRequired()) {
            holdTicks = 0;
            holdSpent = true;
            PacketDistributor.sendToServer(new TrackingPayloads.StartHunt());
        }
    }

    static List<FormattedCharSequence> pingLines() {
        return pingLines;
    }

    static long millisSincePing() {
        return Util.getMillis() - pingShownAtMillis;
    }

    private static void forgetPing() {
        pingLines = List.of();
        WispTrail.stop();
    }

    static boolean isHolding() {
        return holdTicks > 0;
    }

    static float holdProgress() {
        return holdTicksRequired() == 0 ? 1f : Math.min(1f, (float) holdTicks / holdTicksRequired());
    }

    static Optional<TrackingState> state() {
        return state;
    }

    static TrackingPayloads.HuntUpdate hunt() {
        return hunt;
    }

    private static boolean explainWhyHuntCannotStart(long gameTime) {
        Optional<ResourceLocation> creature = trackedCreature();
        if (creature.isEmpty()) {
            showMessage(Component.translatable("tracking.beasts_and_bounties.not_tracking"));
            return false;
        }
        if (ClientBestiary.bonuses(creature.get()).activeTracking() == 0) {
            showMessage(Component.translatable("tracking.beasts_and_bounties.no_active", ClientBestiary.creatureName(creature.get())));
            return false;
        }
        TrackingState current = state.orElseThrow();
        if (current.isHunting(gameTime)) return false;
        long ticksUntilReady = current.ticksUntilReady(gameTime);
        if (ticksUntilReady > 0) {
            showMessage(Component.translatable("tracking.beasts_and_bounties.cooldown", Math.ceilDiv(ticksUntilReady, SharedConstants.TICKS_PER_SECOND)));
            return false;
        }
        return true;
    }

    private static int holdTicksRequired() {
        return TrackingConfig.HUNT_HOLD_SECONDS.get() * SharedConstants.TICKS_PER_SECOND;
    }

    private static boolean isHunting(long gameTime) {
        return state.filter(current -> current.isHunting(gameTime)).isPresent();
    }

    private static void showMessage(Component message) {
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }
}
