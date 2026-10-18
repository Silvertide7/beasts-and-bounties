package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class Tooltips {
    private static final Sprite SPRITE = Sprite.of("tooltip", 16, 16);
    private static final int BORDER = 3;
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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        SPRITE.drawNineSliced(graphics, textX - PADDING, textY - PADDING, width + PADDING * 2, height + PADDING * 2, BORDER);
        RenderSystem.disableBlend();
        for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), textX, textY + i * LINE_HEIGHT, TEXT_COLOR, true);
        graphics.pose().popPose();
    }
}
