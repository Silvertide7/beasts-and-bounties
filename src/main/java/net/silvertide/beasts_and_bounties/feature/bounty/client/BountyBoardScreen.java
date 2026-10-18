package net.silvertide.beasts_and_bounties.feature.bounty.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.EntityPreview;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.GuiText;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.Sprite;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.Tooltips;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyConfig;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyPayloads;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class BountyBoardScreen extends Screen {
    private static final ResourceLocation BOARD_TEXTURE = BeastsAndBountiesMod.id("textures/gui/bounty_board.png");
    private static final Sprite CARD_SPRITE = Sprite.of("bounty_card", 88, 101);
    private static final Sprite WANTED_SPRITE = Sprite.of("wanted_text", 57, 16);
    private static final Sprite REWARDS_SPRITE = Sprite.of("reward_text", 58, 16);
    private static final Sprite HOVERED_CARD_SPRITE = Sprite.of("bounty_card_highlighted", 88, 101);

    private static final int BOARD_WIDTH = 352;
    private static final int BOARD_HEIGHT = 188;
    private static final int CONTENT_X = 19;
    private static final int CONTENT_Y = 21;
    private static final int CONTENT_WIDTH = 314;
    private static final int CONTENT_HEIGHT = 145;
    private static final int CARD_WIDTH = 88;
    private static final int CARD_HEIGHT = 101;
    private static final int CARD_GAP = 12;
    private static final int CARD_PADDING = 3;
    private static final int HEADING_Y = 6;
    private static final int WANTED_DRAWN_WIDTH = 43;
    private static final int WANTED_DRAWN_HEIGHT = 12;
    private static final int MODEL_BOX_TOP = 20;
    private static final int MODEL_BOX_HEIGHT = 28;
    private static final int NAME_Y = 49;
    private static final int TASK_Y = 56;
    private static final int LINE_GAP = 1;
    private static final float SLIGHTLY_SMALLER = 0.9f;
    private static final int REWARDS_DRAWN_WIDTH = 44;
    private static final int REWARDS_DRAWN_HEIGHT = 12;
    private static final int ITEM_ICON_NATIVE_SIZE = 16;
    private static final int ITEM_ICON_DRAWN_SIZE = 8;
    private static final int ITEM_GAP = 1;
    private static final int DETAIL_RAISE = -2;
    private static final int XP_TO_ITEMS_GAP = 3;
    private static final int REWARDS_HEADING_LOWER = -1;
    private static final int REWARD_TEXT_COLOR = 0x000000;
    private static final int BUTTON_HEIGHT = 12;
    private static final float HOVERED_REWARDS_OPACITY = 0.25f;
    private static final float HOVERED_SCALE = 1.1f;
    private static final float SHADOW_ALPHA = 0.6f;
    private static final int SHADOW_OFFSET = 2;
    private static final int HOVERED_SHADOW_OFFSET = 4;
    private static final int FOOTER_Y = CONTENT_Y + CONTENT_HEIGHT - 12;
    private static final float MODEL_FILL_FRACTION = 0.9f;
    private static final float MODEL_YAW_DEGREES = 180f + 25f;
    private static final float ABOVE_ITEM_COUNTS_Z = 300f;
    private static final int MAX_ITEM_ROWS = 2;
    private static final int TEXT_COLOR = 0x3B2A1A;
    private static final int FOOTER_COLOR = 0xE6D3A3;
    private static final int GLYPH_HEIGHT = 8;
    private static final int SECONDS_PER_MINUTE = 60;

    private enum PosterButton {
        ACCEPT("accept_text", 88, 66),
        ABANDON("abandon_text", 92, 69);

        private final Sprite sprite;
        private final Sprite highlightedSprite;
        private final int width;

        PosterButton(String spriteName, int textureWidth, int drawnWidth) {
            this.sprite = Sprite.of(spriteName, textureWidth, 16);
            this.highlightedSprite = Sprite.of(spriteName + "_highlighted", textureWidth, 16);
            this.width = drawnWidth;
        }
    }

    private record Poster(ResourceLocation entityId, int x, FormattedCharSequence nameLine, float nameScale, List<Component> tooltip, int shownItems, @Nullable LivingEntity model) {
        int centerX() {
            return x + CARD_WIDTH / 2;
        }
    }

    private BountyPayloads.BoardOpened board;
    private final List<Poster> posters = new ArrayList<>();
    private int left;
    private int top;
    private float bodyScale;
    private float detailScale;
    private float experienceScale;
    private float slayScale;

    BountyBoardScreen(BountyPayloads.BoardOpened board) {
        super(Component.translatable("gui.beasts_and_bounties.board.title"));
        this.board = board;
    }

    void show(BountyPayloads.BoardOpened newBoard) {
        board = newBoard;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        left = (width - BOARD_WIDTH) / 2;
        top = (height - BOARD_HEIGHT) / 2;
        bodyScale = GuiText.pixelSnappedBodyScale();
        detailScale = GuiText.pixelSnappedSmallScale() * SLIGHTLY_SMALLER;
        experienceScale = GuiText.pixelSnappedSmallScale();
        slayScale = bodyScale * SLIGHTLY_SMALLER;
        posters.clear();
        Optional<BountyState.ActiveBounty> active = board.state().active();
        List<ResourceLocation> shown = active.map(bounty -> List.of(bounty.entityId()))
                .orElseGet(() -> board.state().canAccept(now()) ? board.offers().stream().map(BountyPayloads.Offer::entityId).toList() : List.of());
        int rowLeft = left + CONTENT_X + (CONTENT_WIDTH - (shown.size() * CARD_WIDTH + (shown.size() - 1) * CARD_GAP)) / 2;
        for (int i = 0; i < shown.size(); i++) posters.add(poster(shown.get(i), rowLeft + i * (CARD_WIDTH + CARD_GAP)));
    }

    private Poster poster(ResourceLocation entityId, int x) {
        Optional<BountyPayloads.Offer> offer = board.offers().stream().filter(candidate -> candidate.entityId().equals(entityId)).findFirst();
        int shownItems = offer.map(this::itemsThatFit).orElse(0);
        List<Component> tooltip = new ArrayList<>();
        Component name = Component.literal(ForgeRegistries.ENTITY_TYPES.getValue(entityId).getDescription().getString().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD);
        float nameScale = nameScaleToFit(name);
        FormattedCharSequence nameLine = name.getVisualOrderText();
        if (font.width(name) * nameScale > nameWidthAvailable()) {
            Component ellipsis = Component.translatable("gui.beasts_and_bounties.board.ellipsis");
            int widthForText = Math.round(nameWidthAvailable() / nameScale) - font.width(ellipsis);
            nameLine = Component.literal(font.substrByWidth(name, widthForText).getString()).append(ellipsis).withStyle(ChatFormatting.BOLD).getVisualOrderText();
            tooltip.add(name);
        }
        if (offer.isPresent() && shownItems < offer.get().items().size()) {
            tooltip.add(Component.translatable("gui.beasts_and_bounties.board.rewards_tooltip").withStyle(ChatFormatting.GRAY));
            if (offer.get().experience() > 0) tooltip.add(Component.translatable("gui.beasts_and_bounties.board.experience", offer.get().experience()));
            tooltip.addAll(offer.get().skillExperience());
            for (ItemStack stack : offer.get().items()) tooltip.add(Component.translatable("gui.beasts_and_bounties.board.item_line", stack.getCount(), stack.getHoverName()));
        }
        List<Component> habitat = offer.map(BountyPayloads.Offer::habitat).orElse(List.of());
        if (!habitat.isEmpty()) {
            tooltip.add(Component.translatable("gui.beasts_and_bounties.board.habitat").withStyle(ChatFormatting.GRAY));
            tooltip.addAll(habitat);
        }
        Entity entity = ForgeRegistries.ENTITY_TYPES.getValue(entityId).create(Minecraft.getInstance().level);
        return new Poster(entityId, x, nameLine, nameScale, tooltip, shownItems, entity instanceof LivingEntity living ? living : null);
    }

    private static int nameWidthAvailable() {
        return CARD_WIDTH - 2 * CARD_PADDING;
    }

    private float nameScaleToFit(Component name) {
        if (font.width(name) * bodyScale <= nameWidthAvailable()) return bodyScale;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        float snappedFit = (float) (Math.floor(nameWidthAvailable() / (double) font.width(name) * guiScale) / guiScale);
        return Math.max(snappedFit, GuiText.pixelSnappedSmallScale());
    }

    private static int itemsPerRow() {
        return (CARD_WIDTH - 2 * CARD_PADDING + ITEM_GAP) / (ITEM_ICON_DRAWN_SIZE + ITEM_GAP);
    }

    private int itemsThatFit(BountyPayloads.Offer offer) {
        int total = offer.items().size();
        int capacity = itemsPerRow() * MAX_ITEM_ROWS;
        if (total <= capacity) return total;
        float lastRowAvailable = CARD_WIDTH - 2 * CARD_PADDING;
        for (int shown = capacity - 1; shown > 0; shown--) {
            int onLastRow = shown - itemsPerRow() * (MAX_ITEM_ROWS - 1);
            if (itemsWidth(onLastRow) + ITEM_GAP + font.width(moreText(total - shown)) * detailScale <= lastRowAvailable) return shown;
        }
        return 0;
    }

    private List<Component> experienceLine(BountyPayloads.Offer offer) {
        List<Component> parts = new ArrayList<>();
        if (offer.experience() > 0) parts.add(experienceText(offer));
        parts.addAll(offer.skillExperience());
        return parts;
    }

    private static float itemsWidth(int count) {
        return count == 0 ? 0 : count * (ITEM_ICON_DRAWN_SIZE + ITEM_GAP) - ITEM_GAP;
    }

    private static Component experienceText(BountyPayloads.Offer offer) {
        return Component.translatable("gui.beasts_and_bounties.board.experience", offer.experience());
    }

    private static Component moreText(int hidden) {
        return Component.translatable("gui.beasts_and_bounties.board.more_items", hidden);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        graphics.blit(BOARD_TEXTURE, left, top, BOARD_WIDTH, BOARD_HEIGHT, 0, 0, BOARD_WIDTH, BOARD_HEIGHT, BOARD_WIDTH, BOARD_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        long now = now();
        BountyState state = board.state();
        Optional<BountyState.ActiveBounty> active = state.active();
        for (Poster poster : posters) {
            Component task = active.map(bounty -> Component.translatable("gui.beasts_and_bounties.board.progress", bounty.killsSoFar(), bounty.killsRequired()).withStyle(ChatFormatting.DARK_RED))
                    .orElseGet(() -> offerOf(poster).map(offer -> Component.translatable("gui.beasts_and_bounties.board.kills", offer.kills()).withStyle(ChatFormatting.DARK_RED)).orElse(Component.empty()));
            renderPoster(graphics, poster, task, offerOf(poster).orElse(null), mouseX, mouseY);
        }
        Component footer = active.isPresent() ? Component.translatable("gui.beasts_and_bounties.board.active_hint")
                : state.canAccept(now) ? Component.translatable("gui.beasts_and_bounties.board.refresh", duration(Math.max(0, board.periodEndsAt() - now)))
                : Component.translatable("gui.beasts_and_bounties.board.cooldown", duration(state.ticksUntilReady(now)));
        drawFooter(graphics, footer, left + CONTENT_X + CONTENT_WIDTH / 2, top + FOOTER_Y);
        for (Poster poster : posters) {
            if (isOverCard(mouseX, mouseY, poster) && !isOverButton(mouseX, mouseY, poster))
                Tooltips.render(graphics, font, poster.tooltip(), mouseX, mouseY);
        }
    }

    private Optional<BountyPayloads.Offer> offerOf(Poster poster) {
        return board.offers().stream().filter(offer -> offer.entityId().equals(poster.entityId())).findFirst();
    }

    private void renderPoster(GuiGraphics graphics, Poster poster, Component task, @Nullable BountyPayloads.Offer rewards, int mouseX, int mouseY) {
        int x = poster.x();
        int y = cardTop();
        boolean hovered = isOverCard(mouseX, mouseY, poster);
        graphics.pose().pushPose();
        if (hovered) {
            graphics.pose().translate(poster.centerX(), y + CARD_HEIGHT / 2f, 0);
            graphics.pose().scale(HOVERED_SCALE, HOVERED_SCALE, 1f);
            graphics.pose().translate(-poster.centerX(), -(y + CARD_HEIGHT / 2f), 0);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0f, 0f, 0f, SHADOW_ALPHA);
        Sprite cardSprite = hovered ? HOVERED_CARD_SPRITE : CARD_SPRITE;
        int shadowOffset = hovered ? HOVERED_SHADOW_OFFSET : SHADOW_OFFSET;
        cardSprite.draw(graphics, x + shadowOffset, y + shadowOffset);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        cardSprite.draw(graphics, x, y);
        RenderSystem.disableBlend();
        WANTED_SPRITE.drawBlended(graphics, poster.centerX() - WANTED_DRAWN_WIDTH / 2, y + HEADING_Y, WANTED_DRAWN_WIDTH, WANTED_DRAWN_HEIGHT);
        renderModel(graphics, poster, y + MODEL_BOX_TOP);
        float nameY = y + NAME_Y + (GLYPH_HEIGHT * bodyScale - GLYPH_HEIGHT * poster.nameScale()) / 2;
        drawScaledAt(graphics, poster.nameLine(), poster.centerX() - font.width(poster.nameLine()) * poster.nameScale() / 2, nameY, poster.nameScale(), TEXT_COLOR);
        drawCenteredScaled(graphics, task, poster.centerX(), y + TASK_Y, slayScale);
        if (rewards != null) renderRewards(graphics, poster, rewards, y + TASK_Y + GLYPH_HEIGHT * slayScale + LINE_GAP);
        PosterButton button = posterButton();
        if (hovered && button != null) {
            int detailsTop = y + rewardDetailsTop();
            int detailsBottom = y + CARD_HEIGHT - CARD_PADDING;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, ABOVE_ITEM_COUNTS_Z);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f - HOVERED_REWARDS_OPACITY);
            cardSprite.drawRegion(graphics, CARD_PADDING, rewardDetailsTop(), x + CARD_PADDING, detailsTop, CARD_WIDTH - 2 * CARD_PADDING, detailsBottom - detailsTop);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
            Sprite sprite = isOverButton(mouseX, mouseY, poster) ? button.highlightedSprite : button.sprite;
            sprite.drawBlended(graphics, poster.centerX() - button.width / 2, y + buttonTop(), button.width, BUTTON_HEIGHT);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    private int rewardDetailsTop() {
        return Math.round(TASK_Y + GLYPH_HEIGHT * slayScale + LINE_GAP + REWARDS_HEADING_LOWER + REWARDS_DRAWN_HEIGHT + LINE_GAP - DETAIL_RAISE);
    }

    private int buttonTop() {
        return (rewardDetailsTop() + CARD_HEIGHT - CARD_PADDING - BUTTON_HEIGHT) / 2;
    }

    private void renderRewards(GuiGraphics graphics, Poster poster, BountyPayloads.Offer offer, float y) {
        y += REWARDS_HEADING_LOWER;
        REWARDS_SPRITE.drawBlended(graphics, poster.centerX() - REWARDS_DRAWN_WIDTH / 2, Math.round(y), REWARDS_DRAWN_WIDTH, REWARDS_DRAWN_HEIGHT);
        y += REWARDS_DRAWN_HEIGHT + LINE_GAP - DETAIL_RAISE;
        List<Component> experience = experienceLine(offer);
        if (!experience.isEmpty()) {
            float width = -XP_TO_ITEMS_GAP;
            for (Component part : experience) width += font.width(part) * experienceScale + XP_TO_ITEMS_GAP;
            float x = poster.centerX() - width / 2;
            for (Component part : experience) {
                drawScaledAt(graphics, part, x, y, experienceScale);
                x += font.width(part) * experienceScale + XP_TO_ITEMS_GAP;
            }
            y += GLYPH_HEIGHT * experienceScale + LINE_GAP;
        }
        List<ItemStack> shown = offer.items().subList(0, poster.shownItems());
        int hidden = offer.items().size() - shown.size();
        for (int start = 0; start < shown.size(); start += itemsPerRow()) {
            List<ItemStack> row = shown.subList(start, Math.min(shown.size(), start + itemsPerRow()));
            boolean lastRow = start + itemsPerRow() >= shown.size();
            float moreWidth = lastRow && hidden > 0 ? ITEM_GAP + font.width(moreText(hidden)) * detailScale : 0;
            float x = poster.centerX() - (itemsWidth(row.size()) + moreWidth) / 2;
            renderItemRow(graphics, x, row, y);
            if (lastRow && hidden > 0) drawScaledAt(graphics, moreText(hidden), x + itemsWidth(row.size()) + ITEM_GAP, y + (ITEM_ICON_DRAWN_SIZE - GLYPH_HEIGHT * detailScale) / 2, detailScale);
            y += ITEM_ICON_DRAWN_SIZE + ITEM_GAP;
        }
    }

    private void renderItemRow(GuiGraphics graphics, float x, List<ItemStack> items, float y) {
        float iconScale = (float) ITEM_ICON_DRAWN_SIZE / ITEM_ICON_NATIVE_SIZE;
        for (ItemStack stack : items) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(iconScale, iconScale, 1f);
            graphics.renderItem(stack, 0, 0);
            graphics.renderItemDecorations(font, stack, 0, 0);
            graphics.pose().popPose();
            x += ITEM_ICON_DRAWN_SIZE + ITEM_GAP;
        }
    }


    private void drawScaledAt(GuiGraphics graphics, Component text, float x, float y, float scale) {
        drawScaledAt(graphics, text.getVisualOrderText(), x, y, scale, REWARD_TEXT_COLOR);
    }

    private void drawScaledAt(GuiGraphics graphics, FormattedCharSequence text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void renderModel(GuiGraphics graphics, Poster poster, int boxTop) {
        LivingEntity model = poster.model();
        if (model == null) return;
        model.yBodyRot = MODEL_YAW_DEGREES;
        model.yBodyRotO = MODEL_YAW_DEGREES;
        model.yHeadRot = MODEL_YAW_DEGREES;
        model.yHeadRotO = MODEL_YAW_DEGREES;
        model.setYRot(MODEL_YAW_DEGREES);
        model.setXRot(0);
        float largestDimension = Math.max(model.getBbWidth(), model.getBbHeight());
        float pixelsPerBlock = MODEL_BOX_HEIGHT * MODEL_FILL_FRACTION / largestDimension;
        Vector3f scissorMin = graphics.pose().last().pose().transformPosition(poster.x() + CARD_PADDING, boxTop, 0, new Vector3f());
        Vector3f scissorMax = graphics.pose().last().pose().transformPosition(poster.x() + CARD_WIDTH - CARD_PADDING, boxTop + MODEL_BOX_HEIGHT, 0, new Vector3f());
        graphics.enableScissor(Math.round(scissorMin.x), Math.round(scissorMin.y), Math.round(scissorMax.x), Math.round(scissorMax.y));
        EntityPreview.render(graphics, poster.centerX(), boxTop + MODEL_BOX_HEIGHT / 2f, pixelsPerBlock, model, new SepiaBufferSource(graphics.bufferSource()));
        graphics.disableScissor();
    }

    private void drawCenteredScaled(GuiGraphics graphics, Component text, int centerX, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, TEXT_COLOR, false);
        graphics.pose().popPose();
    }

    private void drawFooter(GuiGraphics graphics, Component text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, FOOTER_COLOR, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Poster poster : posters) {
            if (!isOverButton(mouseX, mouseY, poster)) continue;
            Network.sendToServer(posterButton() == PosterButton.ABANDON ? new BountyPayloads.AbandonBounty() : new BountyPayloads.AcceptBounty(poster.entityId()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static Component duration(long ticks) {
        long seconds = (ticks + SharedConstants.TICKS_PER_SECOND - 1) / SharedConstants.TICKS_PER_SECOND;
        long minutes = (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE;
        if (seconds >= SECONDS_PER_MINUTE) return Component.translatable("gui.beasts_and_bounties.time.minutes" + (minutes == 1 ? ".one" : ""), minutes);
        return Component.translatable("gui.beasts_and_bounties.time.seconds" + (seconds == 1 ? ".one" : ""), seconds);
    }

    private long now() {
        return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
    }

    private int cardTop() {
        return top + CONTENT_Y + (CONTENT_HEIGHT - CARD_HEIGHT) / 2;
    }

    private @Nullable PosterButton posterButton() {
        if (board.state().active().isEmpty()) return PosterButton.ACCEPT;
        return BountyConfig.ALLOW_ABANDON.get() ? PosterButton.ABANDON : null;
    }

    private boolean isOverButton(double mouseX, double mouseY, Poster poster) {
        PosterButton button = posterButton();
        if (button == null) return false;
        float cardCenterY = cardTop() + CARD_HEIGHT / 2f;
        float buttonCenterY = cardTop() + buttonTop() + BUTTON_HEIGHT / 2f;
        float hoveredCenterY = cardCenterY + (buttonCenterY - cardCenterY) * HOVERED_SCALE;
        float width = button.width * HOVERED_SCALE;
        float height = BUTTON_HEIGHT * HOVERED_SCALE;
        return mouseX >= poster.centerX() - width / 2 && mouseX < poster.centerX() + width / 2
                && mouseY >= hoveredCenterY - height / 2 && mouseY < hoveredCenterY + height / 2;
    }

    private boolean isOverCard(double mouseX, double mouseY, Poster poster) {
        return mouseX >= poster.x() && mouseX < poster.x() + CARD_WIDTH && mouseY >= cardTop() && mouseY < cardTop() + CARD_HEIGHT;
    }
}
