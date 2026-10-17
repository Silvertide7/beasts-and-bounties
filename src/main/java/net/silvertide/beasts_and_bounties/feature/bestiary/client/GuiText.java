package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import net.minecraft.client.Minecraft;

public final class GuiText {
    public static final float BODY_TARGET_SCALE = 0.75f;
    private static final float SMALL_TARGET_SCALE = 0.5f;

    private GuiText() {}

    public static float pixelSnappedBodyScale() {
        return pixelSnapped(BODY_TARGET_SCALE);
    }

    public static float pixelSnappedSmallScale() {
        return pixelSnapped(SMALL_TARGET_SCALE);
    }

    private static float pixelSnapped(float targetScale) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return (float) (Math.max(1, Math.round(guiScale * targetScale)) / guiScale);
    }
}
