package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.SharedConstants;

public enum PassiveTrackingLevel {
    SCENT(4, false, false),
    TRAIL(8, true, false),
    TRACK(8, true, true);

    private final int directions;
    private final boolean showsDistance;
    private final boolean showsWispTrail;

    PassiveTrackingLevel(int directions, boolean showsDistance, boolean showsWispTrail) {
        this.directions = directions;
        this.showsDistance = showsDistance;
        this.showsWispTrail = showsWispTrail;
    }

    public static PassiveTrackingLevel of(int level) {
        return values()[level - 1];
    }

    public int level() {
        return ordinal() + 1;
    }

    public int range() {
        return config().range().get();
    }

    public int pingIntervalTicks() {
        return config().pingIntervalSeconds().get() * SharedConstants.TICKS_PER_SECOND;
    }

    public int directions() {
        return directions;
    }

    public boolean showsDistance() {
        return showsDistance;
    }

    public boolean showsWispTrail() {
        return showsWispTrail;
    }

    private TrackingConfig.PassiveLevel config() {
        return TrackingConfig.PASSIVE_LEVELS[ordinal()];
    }
}
