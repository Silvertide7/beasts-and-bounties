package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.SharedConstants;

public enum ActiveTrackingLevel {
    HUNT(Glow.NONE),
    SIGHT(Glow.NEAREST),
    MARK(Glow.ALL);

    public enum Glow { NONE, NEAREST, ALL }

    private final Glow glow;

    ActiveTrackingLevel(Glow glow) {
        this.glow = glow;
    }

    public static ActiveTrackingLevel of(int level) {
        return values()[level - 1];
    }

    public static int glowRange() {
        return TrackingConfig.GLOW_RANGE.get();
    }

    public static int scanIntervalTicks() {
        return TrackingConfig.HUNT_SCAN_INTERVAL_TICKS.get();
    }

    public long durationTicks() {
        return (long) config().durationSeconds().get() * SharedConstants.TICKS_PER_SECOND;
    }

    public long cooldownTicks() {
        return (long) config().cooldownSeconds().get() * SharedConstants.TICKS_PER_SECOND;
    }

    public int arrowRange() {
        return config().arrowRange().get();
    }

    public Glow glow() {
        return glow;
    }

    private TrackingConfig.ActiveLevel config() {
        return TrackingConfig.ACTIVE_LEVELS[ordinal()];
    }
}
