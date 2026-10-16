package net.silvertide.beasts_and_bounties.feature.bounty.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.ClientBestiary;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.GuiText;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.Tooltips;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyConfig;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyPayloads;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class BountyBoardScreen extends Screen {
    private static final ResourceLocation BOARD_TEXTURE = BeastsAndBountiesMod.id("textures/gui/bounty_board.png");
    private static final ResourceLocation CARD_SPRITE = BeastsAndBountiesMod.id("bestiary/bounty_card");
    private static final ResourceLocation BUTTON_SPRITE = BeastsAndBountiesMod.id("bestiary/bounty_button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE = BeastsAndBountiesMod.id("bestiary/bounty_button_highlighted");

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
    private static final int HEADING_Y = 8;
    private static final int MODEL_BOX_TOP = 17;
    private static final int MODEL_BOX_HEIGHT = 28;
    private static final int NAME_Y = 46;
    private static final int TASK_Y = 55;
    private static final int REWARD_LABEL_Y = 63;
    private static final int REWARD_Y = 71;
    private static final int REWARD_ICON_SIZE = 16;
    private static final float REWARD_ICON_SCALE = 0.75f;
    private static final int REWARD_GAP = 2;
    private static final int BUTTON_WIDTH = 42;
    private static final int BUTTON_HEIGHT = 14;
    private static final float HOVERED_SCALE = 1.1f;
    private static final float SHADOW_ALPHA = 0.6f;
    private static final int SHADOW_OFFSET = 2;
    private static final int FOOTER_Y = CONTENT_Y + CONTENT_HEIGHT - 10;
    private static final float MODEL_FILL_FRACTION = 0.9f;
    private static final float MODEL_YAW_DEGREES = 180f + 25f;
    private static final float MODEL_Z = 50f;
    private static final int FULL_BRIGHT = 15728880;
    private static final int TEXT_COLOR = 0x3B2A1A;
    private static final int FOOTER_COLOR = 0xE6D3A3;
    private static final int BUTTON_TEXT_COLOR = 0xFFFFFF;
    private static final int GLYPH_HEIGHT = 8;
    private static final int SECONDS_PER_MINUTE = 60;

    private record Poster(ResourceLocation entityId, int x, FormattedCharSequence nameLine, List<Component> tooltip, @Nullable LivingEntity model) {
        int centerX() {
            return x + CARD_WIDTH / 2;
        }
    }

    private final BountyPayloads.BoardOpened board;
    private final List<Poster> posters = new ArrayList<>();
    private int left;
    private int top;
    private float bodyScale;

    BountyBoardScreen(BountyPayloads.BoardOpened board) {
        super(Component.translatable("gui.beasts_and_bounties.board.title"));
        this.board = board;
    }

    @Override
    protected void init() {
        left = (width - BOARD_WIDTH) / 2;
        top = (height - BOARD_HEIGHT) / 2;
        bodyScale = GuiText.pixelSnappedBodyScale();
        posters.clear();
        Optional<BountyState.ActiveBounty> active = board.state().active();
        List<ResourceLocation> shown = active.map(bounty -> List.of(bounty.entityId()))
                .orElseGet(() -> board.state().canAccept(now()) ? board.offers().stream().map(BountyPayloads.Offer::entityId).toList() : List.of());
        int rowLeft = left + CONTENT_X + (CONTENT_WIDTH - (shown.size() * CARD_WIDTH + (shown.size() - 1) * CARD_GAP)) / 2;
        for (int i = 0; i < shown.size(); i++) posters.add(poster(shown.get(i), rowLeft + i * (CARD_WIDTH + CARD_GAP)));
    }

    private Poster poster(ResourceLocation entityId, int x) {
        List<Component> habitat = board.offers().stream().filter(offer -> offer.entityId().equals(entityId)).findFirst()
                .map(BountyPayloads.Offer::habitat).orElse(List.of());
        List<Component> tooltip = new ArrayList<>();
        if (!habitat.isEmpty()) {
            tooltip.add(Component.translatable("gui.beasts_and_bounties.board.habitat").withStyle(ChatFormatting.GRAY));
            tooltip.addAll(habitat);
        }
        Entity entity = BuiltInRegistries.ENTITY_TYPE.get(entityId).create(Minecraft.getInstance().level);
        return new Poster(entityId, x, font.split(Component.literal(ClientBestiary.creatureName(entityId).getString().toUpperCase(Locale.ROOT)), CARD_WIDTH - 2 * CARD_PADDING).getFirst(), tooltip,
                entity instanceof LivingEntity living ? living : null);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        graphics.blit(BOARD_TEXTURE, left, top, BOARD_WIDTH, BOARD_HEIGHT, 0, 0, BOARD_WIDTH, BOARD_HEIGHT, BOARD_WIDTH, BOARD_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        long now = now();
        BountyState state = board.state();
        Optional<BountyState.ActiveBounty> active = state.active();
        for (Poster poster : posters) {
            Component task = active.map(bounty -> Component.translatable("gui.beasts_and_bounties.board.progress", bounty.killsSoFar(), bounty.killsRequired()).withStyle(ChatFormatting.DARK_RED))
                    .orElseGet(() -> offerOf(poster).map(offer -> Component.translatable("gui.beasts_and_bounties.board.kills", offer.kills())).orElse(Component.empty()));
            Component buttonLabel = active.isPresent()
                    ? (BountyConfig.ALLOW_ABANDON.get() ? Component.translatable("gui.beasts_and_bounties.board.abandon") : null)
                    : Component.translatable("gui.beasts_and_bounties.board.accept");
            renderPoster(graphics, poster, task, active.isEmpty() ? offerOf(poster).orElse(null) : null, buttonLabel, mouseX, mouseY);
        }
        Component footer = active.isPresent() ? Component.translatable("gui.beasts_and_bounties.board.active_hint")
                : state.canAccept(now) ? Component.translatable("gui.beasts_and_bounties.board.refresh", duration(Math.max(0, board.periodEndsAt() - now)))
                : Component.translatable("gui.beasts_and_bounties.board.cooldown", duration(state.ticksUntilReady(now)));
        drawCentered(graphics, footer, left + CONTENT_X + CONTENT_WIDTH / 2, top + FOOTER_Y, FOOTER_COLOR, true);
        for (Poster poster : posters) {
            if (isOver(mouseX, mouseY, poster.x(), cardTop(), CARD_WIDTH, CARD_HEIGHT) && !isOver(mouseX, mouseY, buttonX(poster), buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
                Tooltips.render(graphics, font, poster.tooltip(), mouseX, mouseY);
        }
    }

    private Optional<BountyPayloads.Offer> offerOf(Poster poster) {
        return board.offers().stream().filter(offer -> offer.entityId().equals(poster.entityId())).findFirst();
    }

    private void renderPoster(GuiGraphics graphics, Poster poster, Component task, @Nullable BountyPayloads.Offer rewards, @Nullable Component buttonLabel, int mouseX, int mouseY) {
        int x = poster.x();
        int y = cardTop();
        boolean hovered = isOver(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);
        graphics.pose().pushPose();
        if (hovered) {
            graphics.pose().translate(poster.centerX(), y + CARD_HEIGHT / 2f, 0);
            graphics.pose().scale(HOVERED_SCALE, HOVERED_SCALE, 1f);
            graphics.pose().translate(-poster.centerX(), -(y + CARD_HEIGHT / 2f), 0);
        }
        RenderSystem.setShaderColor(0f, 0f, 0f, SHADOW_ALPHA);
        graphics.blitSprite(CARD_SPRITE, x + SHADOW_OFFSET, y + SHADOW_OFFSET, CARD_WIDTH, CARD_HEIGHT);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blitSprite(CARD_SPRITE, x, y, CARD_WIDTH, CARD_HEIGHT);
        drawCentered(graphics, Component.translatable("gui.beasts_and_bounties.board.wanted").withStyle(ChatFormatting.BOLD), poster.centerX(), y + HEADING_Y, ChatFormatting.DARK_RED.getColor(), false);
        renderModel(graphics, poster, y + MODEL_BOX_TOP);
        graphics.drawString(font, poster.nameLine(), poster.centerX() - font.width(poster.nameLine()) / 2, y + NAME_Y, TEXT_COLOR, false);
        drawCenteredScaled(graphics, task, poster.centerX(), y + TASK_Y, TEXT_COLOR);
        if (rewards != null) renderRewards(graphics, poster, rewards, y + REWARD_Y);
        graphics.pose().popPose();
        if (hovered && buttonLabel != null) renderButton(graphics, buttonLabel, buttonX(poster), buttonY(), mouseX, mouseY);
    }

    private void renderRewards(GuiGraphics graphics, Poster poster, BountyPayloads.Offer offer, int y) {
        drawCenteredScaled(graphics, Component.translatable("gui.beasts_and_bounties.board.reward"), poster.centerX(), y - REWARD_Y + REWARD_LABEL_Y, TEXT_COLOR);
        Component experience = Component.translatable("gui.beasts_and_bounties.board.experience", offer.experience());
        int iconsWidth = offer.items().size() * REWARD_ICON_SIZE;
        int totalWidth = iconsWidth + (offer.experience() > 0 ? REWARD_GAP + font.width(experience) : 0);
        graphics.pose().pushPose();
        graphics.pose().translate(poster.centerX(), y, 0);
        graphics.pose().scale(REWARD_ICON_SCALE, REWARD_ICON_SCALE, 1f);
        int iconX = -totalWidth / 2;
        for (ItemStack stack : offer.items()) {
            graphics.renderItem(stack, iconX, 0);
            graphics.renderItemDecorations(font, stack, iconX, 0);
            iconX += REWARD_ICON_SIZE;
        }
        if (offer.experience() > 0) graphics.drawString(font, experience, iconX + REWARD_GAP, (REWARD_ICON_SIZE - GLYPH_HEIGHT) / 2, ChatFormatting.DARK_GREEN.getColor(), false);
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
        graphics.enableScissor(poster.x() - CARD_WIDTH / 8, boxTop - MODEL_BOX_HEIGHT / 4, poster.x() + CARD_WIDTH + CARD_WIDTH / 8, boxTop + MODEL_BOX_HEIGHT + MODEL_BOX_HEIGHT / 4);
        graphics.pose().pushPose();
        graphics.pose().translate(poster.centerX(), boxTop + MODEL_BOX_HEIGHT / 2f, MODEL_Z);
        graphics.pose().scale(pixelsPerBlock, pixelsPerBlock, -pixelsPerBlock);
        graphics.pose().translate(0, model.getBbHeight() / 2f, 0);
        graphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI));
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        SepiaBufferSource sepia = new SepiaBufferSource(graphics.bufferSource());
        RenderSystem.runAsFancy(() -> dispatcher.render(model, 0, 0, 0, 0f, 1f, graphics.pose(), sepia, FULL_BRIGHT));
        graphics.flush();
        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();
        Lighting.setupFor3DItems();
        graphics.disableScissor();
    }

    private void renderButton(GuiGraphics graphics, Component label, int x, int y, int mouseX, int mouseY) {
        boolean hovered = isOver(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        graphics.blitSprite(hovered ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        drawCentered(graphics, label, x + BUTTON_WIDTH / 2, y + (BUTTON_HEIGHT - GLYPH_HEIGHT) / 2, BUTTON_TEXT_COLOR, true);
    }

    private void drawCenteredScaled(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(bodyScale, bodyScale, 1f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private void drawCentered(GuiGraphics graphics, Component text, int centerX, int y, int color, boolean shadow) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, shadow);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean active = board.state().active().isPresent();
        if (active && !BountyConfig.ALLOW_ABANDON.get()) return super.mouseClicked(mouseX, mouseY, button);
        for (Poster poster : posters) {
            if (!isOver(mouseX, mouseY, buttonX(poster), buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) continue;
            PacketDistributor.sendToServer(active ? new BountyPayloads.AbandonBounty() : new BountyPayloads.AcceptBounty(poster.entityId()));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static Component duration(long ticks) {
        long seconds = Math.ceilDiv(ticks, SharedConstants.TICKS_PER_SECOND);
        return seconds >= SECONDS_PER_MINUTE
                ? Component.translatable("gui.beasts_and_bounties.time.minutes", Math.ceilDiv(seconds, SECONDS_PER_MINUTE))
                : Component.translatable("gui.beasts_and_bounties.time.seconds", seconds);
    }

    private long now() {
        return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
    }

    private int cardTop() {
        return top + CONTENT_Y + (CONTENT_HEIGHT - CARD_HEIGHT) / 2;
    }

    private int buttonX(Poster poster) {
        return poster.centerX() - BUTTON_WIDTH / 2;
    }

    private int buttonY() {
        return cardTop() + CARD_HEIGHT - BUTTON_HEIGHT - CARD_PADDING;
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
