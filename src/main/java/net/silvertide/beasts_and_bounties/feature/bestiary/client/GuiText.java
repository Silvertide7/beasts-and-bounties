package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import net.minecraft.client.Minecraft;

public final class GuiText {
    public static final float BODY_TARGET_SCALE = 0.75f;

    private GuiText() {}

    public static float pixelSnappedBodyScale() {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return (float) (Math.max(1, Math.round(guiScale * BODY_TARGET_SCALE)) / guiScale);
    }
}
