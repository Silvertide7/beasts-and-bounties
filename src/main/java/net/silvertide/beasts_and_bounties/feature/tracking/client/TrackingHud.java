package net.silvertide.beasts_and_bounties.feature.tracking.client;

import com.mojang.math.Axis;
import net.minecraft.SharedConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.ClientBestiary;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingClientConfig;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingDirection;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingHeight;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingPayloads;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingState;

import java.util.List;

final class TrackingHud {
    private static final ResourceLocation RING_SPRITE = BeastsAndBountiesMod.id("bestiary/tracking_ring");
    private static final ResourceLocation ARROW_SPRITE = BeastsAndBountiesMod.id("bestiary/tracking_arrow");
    private static final ResourceLocation ABOVE_SPRITE = BeastsAndBountiesMod.id("bestiary/tracking_up");
    private static final ResourceLocation BELOW_SPRITE = BeastsAndBountiesMod.id("bestiary/tracking_down");
    private static final ResourceLocation TIMER_SPRITE = BeastsAndBountiesMod.id("bestiary/tracking_timer");

    private static final int RING_SIZE = 44;
    private static final int RING_RADIUS = 20;
    private static final int ARROW_SIZE = 16;
    private static final int HEIGHT_MARKER_WIDTH = 7;
    private static final int HEIGHT_MARKER_HEIGHT = 5;
    private static final int HEIGHT_MARKER_GAP = 2;

    private static final int STATUS_OFFSET_FROM_BOTTOM = 84;
    static final int PING_MAX_WIDTH = 280;
    static final int PING_MARGIN = 20;
    private static final int PING_BOTTOM_OFFSET = 100;
    private static final int PING_LINE_HEIGHT = 10;
    private static final int PING_COLOR = 0xE6D3A3;
    private static final long PING_FADE_MILLIS = 2000L;
    private static final int MIN_VISIBLE_ALPHA = 8;
    private static final int OPAQUE = 255;
    private static final int TIMER_SIZE = 8;
    private static final int TIMER_GAP = 3;
    private static final int HUNT_TIMER_COLOR = 0x9CE07A;
    private static final int HOLD_TEXT_COLOR = 0xE6D3A3;
    private static final int HOLD_BAR_WIDTH = 60;
    private static final int HOLD_BAR_HEIGHT = 2;
    private static final int HOLD_BAR_GAP = 3;
    private static final int HOLD_BAR_BACKGROUND = 0x80000000;
    private static final int HOLD_BAR_FILL = 0xFFE6D3A3;
    private static final SecondsText timerText = new SecondsText("tracking.beasts_and_bounties.timer");

    private TrackingHud() {}

    static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (minecraft.options.hideGui || player == null || minecraft.level == null) return;
        renderPing(graphics, minecraft);
        long now = minecraft.level.getGameTime();
        ClientTracking.state().filter(state -> state.isHunting(now)).ifPresent(state -> {
            renderHuntCompass(graphics, minecraft, player, deltaTracker.getGameTimeDeltaPartialTick(true));
            renderHuntTimer(graphics, minecraft, state.huntEndsAt() - now);
        });
        if (ClientTracking.isHolding()) renderHold(graphics, minecraft);
    }

    static Component pingText(TrackingPayloads.PassivePing ping) {
        Component direction = Component.translatable("tracking.beasts_and_bounties.direction." + ping.direction().key());
        Component whereabouts = ping.height()
                .<Component>map(height -> Component.translatable("tracking.beasts_and_bounties.whereabouts",
                        Component.translatable("tracking.beasts_and_bounties.height." + height.key()), direction))
                .orElse(direction);
        Component name = ClientBestiary.creatureName(ping.creature());
        String key = "tracking.beasts_and_bounties.ping." + ping.level();
        return ping.roundedDistance()
                .map(distance -> Component.translatable(key, name, distance, whereabouts))
                .orElseGet(() -> Component.translatable(key, name, whereabouts));
    }

    private static void renderPing(GuiGraphics graphics, Minecraft minecraft) {
        List<FormattedCharSequence> lines = ClientTracking.pingLines();
        if (lines.isEmpty()) return;
        long holdMillis = Math.round(TrackingClientConfig.PING_TEXT_SECONDS.get() * 1000);
        int alpha = fadingAlpha(ClientTracking.millisSincePing(), holdMillis, PING_FADE_MILLIS);
        if (alpha < MIN_VISIBLE_ALPHA) return;
        int top = graphics.guiHeight() - PING_BOTTOM_OFFSET - lines.size() * PING_LINE_HEIGHT;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            graphics.drawString(minecraft.font, line, (graphics.guiWidth() - minecraft.font.width(line)) / 2, top + i * PING_LINE_HEIGHT, (alpha << 24) | PING_COLOR, true);
        }
    }

    private static int fadingAlpha(long ageMillis, long holdMillis, long fadeMillis) {
        if (ageMillis < 0 || ageMillis > holdMillis + fadeMillis) return 0;
        float fade = ageMillis <= holdMillis ? 1f : 1f - (float) (ageMillis - holdMillis) / fadeMillis;
        return (int) (fade * OPAQUE);
    }

    private static void renderHuntCompass(GuiGraphics graphics, Minecraft minecraft, Player player, float partialTick) {
        ClientTracking.hunt().target().ifPresent(target -> {
            Entity live = minecraft.level.getEntity(target.entityId());
            Vec3 targetPosition = live != null ? live.getPosition(partialTick) : target.position();
            Vec3 playerPosition = player.getPosition(partialTick);
            float relativeYaw = Mth.wrapDegrees(TrackingDirection.yawTowards(playerPosition.x, playerPosition.z, targetPosition.x, targetPosition.z)
                    - player.getViewYRot(partialTick));
            int centerX = graphics.guiWidth() / 2;
            int centerY = graphics.guiHeight() / 2;
            graphics.blitSprite(RING_SPRITE, centerX - RING_SIZE / 2, centerY - RING_SIZE / 2, RING_SIZE, RING_SIZE);

            float bearing = relativeYaw * Mth.DEG_TO_RAD;
            int arrowX = centerX + Math.round(Mth.sin(bearing) * RING_RADIUS);
            int arrowY = centerY - Math.round(Mth.cos(bearing) * RING_RADIUS);
            graphics.pose().pushPose();
            graphics.pose().translate(arrowX, arrowY, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(relativeYaw));
            graphics.blitSprite(ARROW_SPRITE, -ARROW_SIZE / 2, -ARROW_SIZE / 2, ARROW_SIZE, ARROW_SIZE);
            graphics.pose().popPose();

            TrackingHeight.of(targetPosition.y - playerPosition.y).ifPresent(height -> graphics.blitSprite(
                    height == TrackingHeight.ABOVE ? ABOVE_SPRITE : BELOW_SPRITE,
                    arrowX - HEIGHT_MARKER_WIDTH / 2,
                    height == TrackingHeight.ABOVE ? arrowY - ARROW_SIZE / 2 - HEIGHT_MARKER_GAP - HEIGHT_MARKER_HEIGHT : arrowY + ARROW_SIZE / 2 + HEIGHT_MARKER_GAP,
                    HEIGHT_MARKER_WIDTH, HEIGHT_MARKER_HEIGHT));
        });
    }

    private static void renderHuntTimer(GuiGraphics graphics, Minecraft minecraft, long ticksLeft) {
        Component seconds = timerText.textFor(Math.ceilDiv(ticksLeft, SharedConstants.TICKS_PER_SECOND));
        int width = TIMER_SIZE + TIMER_GAP + minecraft.font.width(seconds);
        int x = (graphics.guiWidth() - width) / 2;
        int y = statusY(graphics);
        graphics.blitSprite(TIMER_SPRITE, x, y, TIMER_SIZE, TIMER_SIZE);
        graphics.drawString(minecraft.font, seconds, x + TIMER_SIZE + TIMER_GAP, y, HUNT_TIMER_COLOR, true);
    }

    private static void renderHold(GuiGraphics graphics, Minecraft minecraft) {
        Component text = ClientTracking.trackedCreature()
                .map(creature -> Component.translatable("tracking.beasts_and_bounties.holding", ClientBestiary.creatureName(creature)))
                .orElse(Component.empty());
        int centerX = graphics.guiWidth() / 2;
        int y = statusY(graphics);
        graphics.drawString(minecraft.font, text, centerX - minecraft.font.width(text) / 2, y, HOLD_TEXT_COLOR, true);
        int barX = centerX - HOLD_BAR_WIDTH / 2;
        int barY = y + minecraft.font.lineHeight + HOLD_BAR_GAP;
        graphics.fill(barX, barY, barX + HOLD_BAR_WIDTH, barY + HOLD_BAR_HEIGHT, HOLD_BAR_BACKGROUND);
        graphics.fill(barX, barY, barX + Math.round(HOLD_BAR_WIDTH * ClientTracking.holdProgress()), barY + HOLD_BAR_HEIGHT, HOLD_BAR_FILL);
    }

    private static int statusY(GuiGraphics graphics) {
        return graphics.guiHeight() - STATUS_OFFSET_FROM_BOTTOM;
    }

    private static final class SecondsText {
        private final String key;
        private long seconds = -1;
        private Component text = Component.empty();

        private SecondsText(String key) {
            this.key = key;
        }

        Component textFor(long newSeconds) {
            if (newSeconds != seconds) {
                seconds = newSeconds;
                text = Component.translatable(key, newSeconds);
            }
            return text;
        }
    }
}
