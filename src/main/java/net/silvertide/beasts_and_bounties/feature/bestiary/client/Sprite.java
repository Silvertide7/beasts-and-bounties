package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public record Sprite(ResourceLocation texture, int width, int height) {
    public static Sprite of(String name, int width, int height) {
        return new Sprite(BeastsAndBountiesMod.id("textures/gui/sprites/bestiary/" + name + ".png"), width, height);
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        draw(graphics, x, y, width, height);
    }

    public void draw(GuiGraphics graphics, int x, int y, int drawnWidth, int drawnHeight) {
        graphics.blit(texture, x, y, drawnWidth, drawnHeight, 0, 0, width, height, width, height);
    }

    public void drawBlended(GuiGraphics graphics, int x, int y, int drawnWidth, int drawnHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        draw(graphics, x, y, drawnWidth, drawnHeight);
        RenderSystem.disableBlend();
    }

    public void drawRegion(GuiGraphics graphics, int u, int v, int x, int y, int regionWidth, int regionHeight) {
        graphics.blit(texture, x, y, regionWidth, regionHeight, u, v, regionWidth, regionHeight, width, height);
    }

    public void drawNineSliced(GuiGraphics graphics, int x, int y, int drawnWidth, int drawnHeight, int border) {
        int innerWidth = width - 2 * border;
        int innerHeight = height - 2 * border;
        int middleWidth = drawnWidth - 2 * border;
        int middleHeight = drawnHeight - 2 * border;
        drawRegion(graphics, 0, 0, x, y, border, border);
        drawStretched(graphics, border, 0, x + border, y, middleWidth, border, innerWidth, border);
        drawRegion(graphics, width - border, 0, x + drawnWidth - border, y, border, border);
        drawStretched(graphics, 0, border, x, y + border, border, middleHeight, border, innerHeight);
        drawStretched(graphics, border, border, x + border, y + border, middleWidth, middleHeight, innerWidth, innerHeight);
        drawStretched(graphics, width - border, border, x + drawnWidth - border, y + border, border, middleHeight, border, innerHeight);
        drawRegion(graphics, 0, height - border, x, y + drawnHeight - border, border, border);
        drawStretched(graphics, border, height - border, x + border, y + drawnHeight - border, middleWidth, border, innerWidth, border);
        drawRegion(graphics, width - border, height - border, x + drawnWidth - border, y + drawnHeight - border, border, border);
    }

    private void drawStretched(GuiGraphics graphics, int u, int v, int x, int y, int drawnWidth, int drawnHeight, int regionWidth, int regionHeight) {
        graphics.blit(texture, x, y, drawnWidth, drawnHeight, u, v, regionWidth, regionHeight, width, height);
    }
}
