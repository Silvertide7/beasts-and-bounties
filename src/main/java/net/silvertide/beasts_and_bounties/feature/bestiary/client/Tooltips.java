package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.List;

public final class Tooltips {
    private static final ResourceLocation SPRITE = BeastsAndBountiesMod.id("bestiary/tooltip");
    private static final int PADDING = 4;
    private static final int CURSOR_OFFSET = 12;
    private static final int Z = 400;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private Tooltips() {}

    public static void render(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) return;
        int width = lines.stream().mapToInt(font::width).max().orElse(0);
        int height = lines.size() * LINE_HEIGHT - 1;
        int textX = Math.min(mouseX + CURSOR_OFFSET, graphics.guiWidth() - width - PADDING);
        int textY = mouseY - CURSOR_OFFSET;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, Z);
        graphics.blitSprite(SPRITE, textX - PADDING, textY - PADDING, width + PADDING * 2, height + PADDING * 2);
        for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), textX, textY + i * LINE_HEIGHT, TEXT_COLOR, true);
        graphics.pose().popPose();
    }
}
