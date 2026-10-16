package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraft.util.Mth;

import java.util.Locale;

public enum TrackingDirection {
    AHEAD, AHEAD_RIGHT, RIGHT, BEHIND_RIGHT, BEHIND, BEHIND_LEFT, LEFT, AHEAD_LEFT;

    private static final TrackingDirection[] FOUR_WAY = {AHEAD, RIGHT, BEHIND, LEFT};
    private static final float FULL_TURN = 360f;

    public static TrackingDirection fromRelativeYaw(float relativeYaw, int directions) {
        float sector = FULL_TURN / directions;
        int index = Math.floorMod(Math.round(Mth.wrapDegrees(relativeYaw) / sector), directions);
        return directions == FOUR_WAY.length ? FOUR_WAY[index] : values()[index];
    }

    public static float yawTowards(double fromX, double fromZ, double toX, double toZ) {
        return (float) (Mth.atan2(toZ - fromZ, toX - fromX) * Mth.RAD_TO_DEG) - 90f;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
