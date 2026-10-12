package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.Bonuses;
import net.silvertide.beasts_and_bounties.feature.bestiary.Classification;
import net.silvertide.beasts_and_bounties.feature.bestiary.Knowledge;
import net.silvertide.beasts_and_bounties.feature.bestiary.Progress;
import net.silvertide.beasts_and_bounties.feature.bestiary.Reward;
import net.silvertide.beasts_and_bounties.feature.bestiary.Tier;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BestiaryScreen extends Screen {
    private static final ResourceLocation SPREAD_TEXTURE = BeastsAndBountiesMod.id("textures/gui/bestiary.png");
    private static final int TEXTURE_WIDTH = 1080;
    private static final int TEXTURE_HEIGHT = 665;
    private static final float TEXTURE_PIXELS_PER_GUI_PIXEL = 3.6f;
    private static final int SPREAD_WIDTH = guiPixels(TEXTURE_WIDTH);
    private static final int SPREAD_HEIGHT = guiPixels(TEXTURE_HEIGHT);

    private static final Area SEARCH_AREA = Area.fromTexture(118, 57, 350, 30);
    private static final Area LIST_AREA = Area.fromTexture(82, 101, 408, 447);
    private static final Area LIST_FOOTER_AREA = Area.fromTexture(103, 548, 380, 30);
    private static final Area HEADER_AREA = Area.fromTexture(612, 57, 350, 30);
    private static final Area CONTENT_AREA = Area.fromTexture(597, 101, 410, 447);
    private static final Area CONTENT_FOOTER_AREA = Area.fromTexture(588, 548, 370, 30);
    private static final int LINE_HEIGHT = 10;
    private static final int GLYPH_HEIGHT = 8;
    private static final float BODY_TEXT_TARGET_SCALE = 0.75f;

    private static final ResourceLocation CLEAR_SEARCH_SPRITE = BeastsAndBountiesMod.id("bestiary/clear_search");
    private static final ResourceLocation CLEAR_SEARCH_HIGHLIGHTED_SPRITE = BeastsAndBountiesMod.id("bestiary/clear_search_highlighted");
    private static final int CLEAR_SEARCH_SIZE = 7;
    private static final int CLEAR_SEARCH_GAP = 2;
    private static final int TEXT_COLOR = 0x000000;
    private static final int MUTED_TEXT_COLOR = 0x5A4A3A;

    private static final int ROW_HEIGHT = 11;
    private static final int ROWS_PER_PAGE = LIST_AREA.height() / ROW_HEIGHT;
    private static final int ICON_SIZE = 8;
    private static final int ICON_GAP = 2;
    private static final int SELECTED_ROW_COLOR = 0x40000000;
    private static final int HOVERED_ROW_COLOR = 0x20000000;

    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_SPACING = 30;
    private static final int TAB_OVERLAP = 5;
    private static final int SELECTED_TAB_OVERLAP = 3;
    private static final int TAB_ICON_INSET_X = 6;
    private static final int TAB_ICON_INSET_Y = 9;
    private static final int TAB_ICON_SIZE = 16;
    private static final ResourceLocation TOOLTIP_SPRITE = BeastsAndBountiesMod.id("bestiary/tooltip");
    private static final int TOOLTIP_PADDING = 4;
    private static final int TOOLTIP_CURSOR_OFFSET = 12;
    private static final int TOOLTIP_Z = 400;
    private static final int TOOLTIP_TEXT_COLOR = 0xFFFFFF;

    private static final int ARROW_WIDTH = 23;
    private static final int ARROW_HEIGHT = 13;

    private static final int MODEL_BOX_HEIGHT = 58;
    private static final float MODEL_FILL_FRACTION = 0.8f;
    private static final long MODEL_ROTATION_PERIOD_MILLIS = 16000L;
    private static final float PLACEHOLDER_SCALE = 4f;

    private static final Component TITLE = Component.translatable("item.beasts_and_bounties.bestiary");
    private static final Component UNKNOWN_MODEL = Component.literal("?");

    private enum Tab {
        OVERVIEW("overview"),
        KNOWLEDGE("knowledge"),
        REWARDS("rewards");

        private final Component title;
        private final ResourceLocation iconSprite;

        Tab(String name) {
            this.title = Component.translatable("gui.beasts_and_bounties.tab." + name);
            this.iconSprite = BeastsAndBountiesMod.id("bestiary/icon_" + name);
        }
    }

    private record Area(int x, int y, int width, int height) {
        static Area fromTexture(int x, int y, int width, int height) {
            return new Area(guiPixels(x), guiPixels(y), guiPixels(width), guiPixels(height));
        }

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }

    private record Row(ResourceLocation entityId, BestiaryEntry entry, Progress progress, Component name, String searchableName) {}

    private final List<Row> discoveredRows = new ArrayList<>();
    private final List<Row> filteredRows = new ArrayList<>();
    private final List<Tab> visibleTabs = new ArrayList<>();
    private final List<FormattedCharSequence> pageLines = new ArrayList<>();
    private EditBox searchBox;
    private int left;
    private int top;
    private float bodyTextScale = 1;
    private int listPage;
    private int contentPage;
    private Tab selectedTab = Tab.OVERVIEW;
    @Nullable
    private Row selectedRow;
    @Nullable
    private LivingEntity displayedEntity;

    private BestiaryScreen() {
        super(TITLE);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new BestiaryScreen());
    }

    @Override
    protected void init() {
        left = (width - SPREAD_WIDTH) / 2;
        top = (height - SPREAD_HEIGHT + TAB_HEIGHT - SELECTED_TAB_OVERLAP) / 2;
        bodyTextScale = pixelSnappedScale(BODY_TEXT_TARGET_SCALE);
        String previousQuery = searchBox == null ? "" : searchBox.getValue();
        searchBox = new EditBox(font, left + SEARCH_AREA.x(), titleTextY(SEARCH_AREA), SEARCH_AREA.width() - CLEAR_SEARCH_SIZE - CLEAR_SEARCH_GAP, GLYPH_HEIGHT,
                Component.translatable("gui.beasts_and_bounties.search"));
        searchBox.setBordered(false);
        searchBox.setTextColor(TEXT_COLOR);
        searchBox.setTextShadow(false);
        searchBox.setHint(Component.translatable("gui.beasts_and_bounties.search").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setValue(previousQuery);
        searchBox.setResponder(query -> applySearch());
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);
        refresh();
    }

    void refresh() {
        discoveredRows.clear();
        ClientBestiary.entries().forEach((entityId, entry) -> {
            Progress progress = ClientBestiary.progress(entityId);
            if (!entry.isDiscovered(progress)) return;
            Component name = ClientBestiary.displayName(entityId, entry, progress);
            String searchableName = entry.knowledge(progress).name() ? name.getString().toLowerCase(Locale.ROOT) : "";
            discoveredRows.add(new Row(entityId, entry, progress, name, searchableName));
        });
        discoveredRows.sort(Comparator.comparing((Row row) -> row.searchableName().isEmpty())
                .thenComparing(Row::searchableName)
                .thenComparing(Row::entityId));
        applySearch();
    }

    private void applySearch() {
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        filteredRows.clear();
        for (Row row : discoveredRows) if (row.searchableName().contains(query)) filteredRows.add(row);
        listPage = Math.min(listPage, listPageCount() - 1);
        ResourceLocation selectedId = selectedRow == null ? null : selectedRow.entityId();
        select(filteredRows.stream().filter(row -> row.entityId().equals(selectedId)).findFirst()
                .orElse(filteredRows.isEmpty() ? null : filteredRows.getFirst()));
    }

    private void select(@Nullable Row row) {
        boolean entityChanged = row == null || selectedRow == null || !row.entityId().equals(selectedRow.entityId());
        selectedRow = row;
        if (entityChanged) {
            contentPage = 0;
            displayedEntity = null;
        }
        rebuildPage();
    }

    private void selectTab(Tab tab) {
        selectedTab = tab;
        contentPage = 0;
        rebuildPage();
    }

    private void rebuildPage() {
        visibleTabs.clear();
        pageLines.clear();
        if (selectedRow == null) return;

        Knowledge knowledge = selectedRow.entry().knowledge(selectedRow.progress());
        List<Reward> rewards = selectedRow.entry().rewardsEarned(selectedRow.progress()).toList();
        visibleTabs.add(Tab.OVERVIEW);
        if (knowledge.hasDetails()) visibleTabs.add(Tab.KNOWLEDGE);
        if (!rewards.isEmpty()) visibleTabs.add(Tab.REWARDS);
        if (!visibleTabs.contains(selectedTab)) selectedTab = Tab.OVERVIEW;

        switch (selectedTab) {
            case OVERVIEW -> buildOverview(knowledge);
            case KNOWLEDGE -> buildKnowledge(knowledge);
            case REWARDS -> buildRewards(rewards);
        }

        contentPage = Math.min(contentPage, contentPageCount() - 1);
        if (displayedEntity == null && knowledge.model()) displayedEntity = createDisplayEntity(selectedRow.entityId());
    }

    private void buildOverview(Knowledge knowledge) {
        BestiaryEntry entry = selectedRow.entry();
        if (entry.classification() != Classification.NORMAL)
            addLine(Component.translatable("classification.beasts_and_bounties." + entry.classification().getSerializedName())
                    .withStyle(entry.classification().nameColor()));
        knowledge.type().ifPresent(type -> addLine(type.copy().withStyle(ChatFormatting.ITALIC)));
        addLine(Component.translatable("gui.beasts_and_bounties.kills", selectedRow.progress().kills()));
        if (entry.usesBounties()) addLine(Component.translatable("gui.beasts_and_bounties.bounties", selectedRow.progress().bounties()));
        addLine(entry.nextRequirement(selectedRow.progress())
                .map(requirement -> Component.translatable("gui.beasts_and_bounties.next_milestone", formatRequirement(requirement)))
                .orElse(Component.translatable("gui.beasts_and_bounties.complete")));
        knowledge.description().ifPresent(description -> {
            pageLines.add(FormattedCharSequence.EMPTY);
            addLine(description);
        });
    }

    private void buildKnowledge(Knowledge knowledge) {
        knowledge.stats().forEach((stat, value) -> addLine(Component.translatableWithFallback("stat.beasts_and_bounties." + stat, stat)
                .append(": ")
                .append(formatStat(value))));
        addSection("attack_patterns", knowledge.attackPatterns());
        addSection("strengths", knowledge.strengths());
        addSection("weaknesses", knowledge.weaknesses());
        addSection("habitat", knowledge.habitat());
        addSection("notes", knowledge.notes());
    }

    private void buildRewards(List<Reward> rewards) {
        Bonuses bonuses = selectedRow.entry().bonuses(selectedRow.progress());
        List<Component> againstThisCreature = new ArrayList<>();
        addPercentReward(againstThisCreature, "damage_bonus", bonuses.damageMultiplier() - 1);
        addPercentReward(againstThisCreature, "damage_resistance", 1 - bonuses.damageTakenMultiplier());
        addPercentReward(againstThisCreature, "extra_loot", bonuses.extraLootChance());
        addPercentReward(againstThisCreature, "bonus_experience", bonuses.experienceMultiplier() - 1);

        Map<Holder<Attribute>, Map<AttributeModifier.Operation, Double>> attributeTotals = new LinkedHashMap<>();
        for (Reward reward : rewards) {
            if (reward instanceof Reward.AttributeBonus bonus)
                attributeTotals.computeIfAbsent(bonus.attribute(), attribute -> new LinkedHashMap<>()).merge(bonus.operation(), bonus.amount(), Double::sum);
        }
        List<Component> permanent = new ArrayList<>();
        attributeTotals.forEach((attribute, totals) -> totals.forEach((operation, amount) -> permanent.add(formatAttributeBonus(attribute, operation, amount))));

        addRewardSection(againstHeading(), Component.translatable("gui.beasts_and_bounties.rewards.against.hint"), againstThisCreature);
        addRewardSection(Component.translatable("gui.beasts_and_bounties.rewards.permanent"),
                Component.translatable("gui.beasts_and_bounties.rewards.permanent.hint"), permanent);
    }

    private Component againstHeading() {
        if (!selectedRow.entry().knowledge(selectedRow.progress()).name())
            return Component.translatable("gui.beasts_and_bounties.rewards.against.unknown");
        return Component.translatable("gui.beasts_and_bounties.rewards.against", BuiltInRegistries.ENTITY_TYPE.get(selectedRow.entityId()).getDescription());
    }

    private void addRewardSection(Component heading, Component hint, List<Component> lines) {
        if (lines.isEmpty()) return;
        if (!pageLines.isEmpty()) pageLines.add(FormattedCharSequence.EMPTY);
        addLine(heading.copy().withStyle(ChatFormatting.BOLD));
        addLine(hint.copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        lines.forEach(this::addLine);
    }

    private static void addPercentReward(List<Component> lines, String reward, double fraction) {
        if (fraction == 0) return;
        String direction = fraction > 0 ? "more" : "less";
        lines.add(Component.translatable("gui.beasts_and_bounties.reward." + reward + "." + direction,
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(fraction) * 100))
                .withStyle(fraction > 0 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
    }

    private void addSection(String name, List<Component> items) {
        if (items.isEmpty()) return;
        if (!pageLines.isEmpty()) pageLines.add(FormattedCharSequence.EMPTY);
        addLine(Component.translatable("gui.beasts_and_bounties.section." + name).withStyle(ChatFormatting.BOLD));
        for (Component item : items) addLine(Component.literal("• ").append(item));
    }

    private void addLine(Component text) {
        pageLines.addAll(font.split(text, (int) (CONTENT_AREA.width() / bodyTextScale)));
    }

    private static Component formatStat(Either<Double, Tier> value) {
        return value.map(
                number -> Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(number)),
                tier -> Component.translatable("tier.beasts_and_bounties." + tier.getSerializedName()).withStyle(tier.color()));
    }

    private static Component formatRequirement(Progress requirement) {
        Component kills = countOf("kills", requirement.kills());
        Component bounties = countOf("bounties", requirement.bounties());
        if (requirement.bounties() == 0) return kills;
        if (requirement.kills() == 0) return bounties;
        return Component.translatable("gui.beasts_and_bounties.requirement.both", kills, bounties);
    }

    private static Component countOf(String counter, int count) {
        return Component.translatable("gui.beasts_and_bounties.requirement." + counter + (count == 1 ? ".one" : ""), count);
    }

    private static Component formatAttributeBonus(Holder<Attribute> attribute, AttributeModifier.Operation operation, double amount) {
        double displayedAmount = operation == AttributeModifier.Operation.ADD_VALUE ? amount : amount * 100;
        String sign = amount >= 0 ? "plus" : "take";
        return Component.translatable("attribute.modifier." + sign + "." + operation.id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(displayedAmount)),
                        Component.translatable(attribute.value().getDescriptionId()))
                .withStyle(amount >= 0 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED);
    }

    @Nullable
    private static LivingEntity createDisplayEntity(ResourceLocation entityId) {
        Entity entity = BuiltInRegistries.ENTITY_TYPE.get(entityId).create(Minecraft.getInstance().level);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        renderTabs(graphics, false);
        graphics.blit(SPREAD_TEXTURE, left, top, SPREAD_WIDTH, SPREAD_HEIGHT, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        renderTabs(graphics, true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderEntryList(graphics, mouseX, mouseY);
        renderPage(graphics, mouseX, mouseY);
        renderTabTooltip(graphics, mouseX, mouseY);
    }

    private void renderTabs(GuiGraphics graphics, boolean selectedPass) {
        for (int i = 0; i < visibleTabs.size(); i++) {
            Tab tab = visibleTabs.get(i);
            boolean selected = tab == selectedTab;
            if (selected != selectedPass) continue;
            int tabX = tabX(i);
            int tabY = tabY(selected);
            graphics.blitSprite(tabSprite(i, selected), tabX, tabY, TAB_WIDTH, TAB_HEIGHT);
            graphics.blitSprite(tab.iconSprite, tabX + TAB_ICON_INSET_X, tabY + TAB_ICON_INSET_Y, TAB_ICON_SIZE, TAB_ICON_SIZE);
        }
    }

    private ResourceLocation tabSprite(int index, boolean selected) {
        String position = index == 0 ? "left" : index == visibleTabs.size() - 1 ? "right" : "middle";
        return BeastsAndBountiesMod.id("bestiary/tab_" + position + (selected ? "_selected" : ""));
    }

    private void renderTabTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Tab hovered = tabAt(mouseX, mouseY);
        if (hovered == null) return;
        int textX = mouseX + TOOLTIP_CURSOR_OFFSET;
        int textY = mouseY - TOOLTIP_CURSOR_OFFSET;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, TOOLTIP_Z);
        graphics.blitSprite(TOOLTIP_SPRITE, textX - TOOLTIP_PADDING, textY - TOOLTIP_PADDING,
                font.width(hovered.title) + TOOLTIP_PADDING * 2, font.lineHeight + TOOLTIP_PADDING * 2 - 1);
        graphics.drawString(font, hovered.title, textX, textY, TOOLTIP_TEXT_COLOR, true);
        graphics.pose().popPose();
    }

    private void renderEntryList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + LIST_AREA.x();
        int underlineY = titleTextY(SEARCH_AREA) + GLYPH_HEIGHT;
        graphics.fill(left + SEARCH_AREA.x(), underlineY, left + SEARCH_AREA.right(), underlineY + 1, MUTED_TEXT_COLOR | 0xFF000000);
        Row hovered = rowAt(mouseX, mouseY);
        int firstRow = listPage * ROWS_PER_PAGE;
        int lastRow = Math.min(filteredRows.size(), firstRow + ROWS_PER_PAGE);
        for (int i = firstRow; i < lastRow; i++) {
            Row row = filteredRows.get(i);
            int rowY = top + LIST_AREA.y() + (i - firstRow) * ROW_HEIGHT;
            if (row == selectedRow) graphics.fill(x - 1, rowY, x + LIST_AREA.width(), rowY + ROW_HEIGHT, SELECTED_ROW_COLOR);
            else if (row == hovered) graphics.fill(x - 1, rowY, x + LIST_AREA.width(), rowY + ROW_HEIGHT, HOVERED_ROW_COLOR);
            renderNameWithIcon(graphics, row, x, rowY + 2, LIST_AREA.width());
        }
        renderClearSearchButton(graphics, mouseX, mouseY);
        renderPageTurning(graphics, LIST_FOOTER_AREA, listPage, listPageCount(), mouseX, mouseY);
    }

    private void renderClearSearchButton(GuiGraphics graphics, int mouseX, int mouseY) {
        if (searchBox.getValue().isEmpty()) return;
        ResourceLocation sprite = isOverClearSearch(mouseX, mouseY) ? CLEAR_SEARCH_HIGHLIGHTED_SPRITE : CLEAR_SEARCH_SPRITE;
        graphics.blitSprite(sprite, clearSearchX(), clearSearchY(), CLEAR_SEARCH_SIZE, CLEAR_SEARCH_SIZE);
    }

    private boolean isOverClearSearch(double mouseX, double mouseY) {
        return mouseX >= clearSearchX() && mouseX < clearSearchX() + CLEAR_SEARCH_SIZE
                && mouseY >= clearSearchY() && mouseY < clearSearchY() + CLEAR_SEARCH_SIZE;
    }

    private int clearSearchX() {
        return left + SEARCH_AREA.right() - CLEAR_SEARCH_SIZE;
    }

    private int clearSearchY() {
        return titleTextY(SEARCH_AREA);
    }

    private void renderPageTurning(GuiGraphics graphics, Area footer, int page, int pageCount, int mouseX, int mouseY) {
        if (pageCount <= 1) return;
        if (page > 0) renderArrow(graphics, footer, false, mouseX, mouseY);
        if (page < pageCount - 1) renderArrow(graphics, footer, true, mouseX, mouseY);
        Component pageNumber = Component.translatable("gui.beasts_and_bounties.page", page + 1, pageCount);
        graphics.drawString(font, pageNumber, left + footer.x() + (footer.width() - font.width(pageNumber)) / 2,
                top + footer.y() + (ARROW_HEIGHT - font.lineHeight) / 2 + 1, MUTED_TEXT_COLOR, false);
    }

    private void renderArrow(GuiGraphics graphics, Area footer, boolean forward, int mouseX, int mouseY) {
        String sprite = (forward ? "bestiary/page_forward" : "bestiary/page_backward")
                + (isOverArrow(mouseX, mouseY, footer, forward) ? "_highlighted" : "");
        graphics.blitSprite(BeastsAndBountiesMod.id(sprite), arrowX(footer, forward), top + footer.y(), ARROW_WIDTH, ARROW_HEIGHT);
    }

    private void renderNameWithIcon(GuiGraphics graphics, Row row, int x, int y, int width) {
        row.entry().classification().iconSprite().ifPresent(sprite -> graphics.blitSprite(sprite, x, y, ICON_SIZE, ICON_SIZE));
        int nameX = x + ICON_SIZE + ICON_GAP;
        graphics.drawString(font, font.split(row.name(), width - ICON_SIZE - ICON_GAP).getFirst(), nameX, y, TEXT_COLOR, false);
    }

    private void renderPage(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + CONTENT_AREA.x();
        int headerX = left + HEADER_AREA.x();
        int headerY = titleTextY(HEADER_AREA);
        if (selectedRow == null) {
            graphics.drawString(font, Component.translatable("gui.beasts_and_bounties.empty"), headerX, headerY, MUTED_TEXT_COLOR, false);
            return;
        }
        renderNameWithIcon(graphics, selectedRow, headerX, headerY, HEADER_AREA.width());
        if (selectedTab == Tab.OVERVIEW) renderModel(graphics, x, top + CONTENT_AREA.y());
        int firstLine = contentPage * linesPerContentPage();
        int lastLine = Math.min(pageLines.size(), firstLine + linesPerContentPage());
        graphics.pose().pushPose();
        graphics.pose().translate(x, top + contentLinesTop(), 0);
        graphics.pose().scale(bodyTextScale, bodyTextScale, 1);
        for (int i = firstLine; i < lastLine; i++)
            graphics.drawString(font, pageLines.get(i), 0, (i - firstLine) * LINE_HEIGHT, TEXT_COLOR, false);
        graphics.pose().popPose();
        renderPageTurning(graphics, CONTENT_FOOTER_AREA, contentPage, contentPageCount(), mouseX, mouseY);
    }

    private int contentLinesTop() {
        return CONTENT_AREA.y() + (selectedTab == Tab.OVERVIEW ? MODEL_BOX_HEIGHT : 0);
    }

    private int linesPerContentPage() {
        return (int) ((CONTENT_AREA.bottom() - contentLinesTop()) / (LINE_HEIGHT * bodyTextScale));
    }

    private int contentPageCount() {
        return Math.max(1, Math.ceilDiv(pageLines.size(), linesPerContentPage()));
    }

    private int listPageCount() {
        return Math.max(1, Math.ceilDiv(filteredRows.size(), ROWS_PER_PAGE));
    }

    private void renderModel(GuiGraphics graphics, int x, int y) {
        int centerX = x + CONTENT_AREA.width() / 2;
        int centerY = y + MODEL_BOX_HEIGHT / 2;
        if (displayedEntity == null) {
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, centerY, 0);
            graphics.pose().scale(PLACEHOLDER_SCALE, PLACEHOLDER_SCALE, 1);
            graphics.drawString(font, UNKNOWN_MODEL, -font.width(UNKNOWN_MODEL) / 2, -font.lineHeight / 2, MUTED_TEXT_COLOR, false);
            graphics.pose().popPose();
            return;
        }
        float yaw = (Util.getMillis() % MODEL_ROTATION_PERIOD_MILLIS) * 360f / MODEL_ROTATION_PERIOD_MILLIS;
        displayedEntity.yBodyRot = yaw;
        displayedEntity.yBodyRotO = yaw;
        displayedEntity.yHeadRot = yaw;
        displayedEntity.yHeadRotO = yaw;
        displayedEntity.setYRot(yaw);
        displayedEntity.setXRot(0);
        float largestDimension = Math.max(displayedEntity.getBbWidth(), displayedEntity.getBbHeight());
        float pixelsPerBlock = MODEL_BOX_HEIGHT * MODEL_FILL_FRACTION / largestDimension;
        graphics.enableScissor(x, y, x + CONTENT_AREA.width(), y + MODEL_BOX_HEIGHT);
        InventoryScreen.renderEntityInInventory(graphics, centerX, centerY, pixelsPerBlock,
                new Vector3f(0, displayedEntity.getBbHeight() / 2, 0), new Quaternionf().rotateZ((float) Math.PI), null, displayedEntity);
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Tab tab = tabAt(mouseX, mouseY);
        if (tab != null) {
            selectTab(tab);
            return true;
        }
        Row row = rowAt(mouseX, mouseY);
        if (row != null) {
            select(row);
            return true;
        }
        if (turnPageIfArrowClicked(mouseX, mouseY)) return true;
        if (!searchBox.getValue().isEmpty() && isOverClearSearch(mouseX, mouseY)) {
            searchBox.setValue("");
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean turnPageIfArrowClicked(double mouseX, double mouseY) {
        for (boolean rightPage : new boolean[]{false, true}) {
            Area footer = rightPage ? CONTENT_FOOTER_AREA : LIST_FOOTER_AREA;
            int page = rightPage ? contentPage : listPage;
            int pageCount = rightPage ? contentPageCount() : listPageCount();
            int turn = 0;
            if (page > 0 && isOverArrow(mouseX, mouseY, footer, false)) turn = -1;
            if (page < pageCount - 1 && isOverArrow(mouseX, mouseY, footer, true)) turn = 1;
            if (turn == 0) continue;
            if (rightPage) contentPage += turn;
            else listPage += turn;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
            return true;
        }
        return false;
    }

    private boolean isOverArrow(double mouseX, double mouseY, Area footer, boolean forward) {
        int arrowX = arrowX(footer, forward);
        int arrowY = top + footer.y();
        return mouseX >= arrowX && mouseX < arrowX + ARROW_WIDTH && mouseY >= arrowY && mouseY < arrowY + ARROW_HEIGHT;
    }

    private int arrowX(Area footer, boolean forward) {
        return left + (forward ? footer.right() - ARROW_WIDTH : footer.x());
    }

    @Nullable
    private Tab tabAt(double mouseX, double mouseY) {
        for (int i = 0; i < visibleTabs.size(); i++) {
            int tabX = tabX(i);
            int tabY = tabY(visibleTabs.get(i) == selectedTab);
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH && mouseY >= tabY && mouseY < top) return visibleTabs.get(i);
        }
        return null;
    }

    @Nullable
    private Row rowAt(double mouseX, double mouseY) {
        int x = left + LIST_AREA.x();
        int listY = top + LIST_AREA.y();
        if (mouseX < x || mouseX >= x + LIST_AREA.width() || mouseY < listY) return null;
        int rowOnPage = (int) ((mouseY - listY) / ROW_HEIGHT);
        int index = listPage * ROWS_PER_PAGE + rowOnPage;
        return rowOnPage < ROWS_PER_PAGE && index < filteredRows.size() ? filteredRows.get(index) : null;
    }

    private int tabY(boolean selected) {
        return top - TAB_HEIGHT + (selected ? SELECTED_TAB_OVERLAP : TAB_OVERLAP);
    }

    private int tabX(int index) {
        return left + CONTENT_AREA.x() + index * TAB_SPACING;
    }

    private int titleTextY(Area area) {
        return top + area.y() + (area.height() - GLYPH_HEIGHT) / 2;
    }

    private float pixelSnappedScale(float targetScale) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return (float) (Math.max(1, Math.round(guiScale * targetScale)) / guiScale);
    }

    private static int guiPixels(int texturePixels) {
        return Math.round(texturePixels / TEXTURE_PIXELS_PER_GUI_PIXEL);
    }
}
