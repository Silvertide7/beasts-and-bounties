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
import net.silvertide.beasts_and_bounties.feature.bounty.client.ClientBounties;
import net.silvertide.beasts_and_bounties.feature.tracking.client.ClientTracking;
import net.silvertide.beasts_and_bounties.feature.tracking.client.TrackingClientEvents;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    private static final ResourceLocation CLEAR_SEARCH_SPRITE = BeastsAndBountiesMod.id("bestiary/clear_search");
    private static final ResourceLocation CLEAR_SEARCH_HIGHLIGHTED_SPRITE = BeastsAndBountiesMod.id("bestiary/clear_search_highlighted");
    private static final int CLEAR_SEARCH_SIZE = 7;
    private static final int CLEAR_SEARCH_GAP = 2;

    private static final ResourceLocation TRACK_BUTTON_SPRITE = BeastsAndBountiesMod.id("bestiary/track_button");
    private static final ResourceLocation TRACK_BUTTON_HIGHLIGHTED_SPRITE = BeastsAndBountiesMod.id("bestiary/track_button_highlighted");
    private static final ResourceLocation TRACKED_EYE_SPRITE = BeastsAndBountiesMod.id("bestiary/tracked_eye");
    private static final ResourceLocation BOUNTY_MARK_SPRITE = BeastsAndBountiesMod.id("bestiary/bounty_mark");
    private static final ResourceLocation TRACKED_BOUNTY_MARK_SPRITE = BeastsAndBountiesMod.id("bestiary/tracked_bounty_mark");
    private static final int BOUNTY_ROW_COLOR = 0x40C04020;
    private static final int TRACKED_ROW_COLOR = 0x402060C0;
    private static final int TRACK_BUTTON_HEIGHT = 20;
    private static final int TRACK_BUTTON_PADDING = 8;
    private static final int TRACK_BUTTON_GAP = 3;
    private static final int TRACK_BUTTON_TEXT_COLOR = 0xFFFFFF;
    private static final int TRACKED_EYE_SIZE = 9;
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
        REWARDS("rewards"),
        TRACKING("tracking");

        private final Component title;
        private final ResourceLocation iconSprite;

        Tab(String name) {
            this.title = Component.translatable("gui.beasts_and_bounties.tab." + name);
            this.iconSprite = BeastsAndBountiesMod.id("bestiary/icon_" + name);
        }
    }

    private enum TabShape {
        LEFT, MIDDLE, RIGHT;

        private final ResourceLocation sprite = BeastsAndBountiesMod.id("bestiary/tab_" + name().toLowerCase(Locale.ROOT));
        private final ResourceLocation selectedSprite = BeastsAndBountiesMod.id("bestiary/tab_" + name().toLowerCase(Locale.ROOT) + "_selected");

        static TabShape at(int index, int count) {
            return index == 0 ? LEFT : index == count - 1 ? RIGHT : MIDDLE;
        }
    }

    private record TabSlot(Tab tab, TabShape shape) {}

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

    private record Row(ResourceLocation entityId, BestiaryEntry entry, Progress progress, Component name, FormattedCharSequence nameLine, String searchableName) {}

    private static final int ROW_NAME_WIDTH = LIST_AREA.width() - ICON_SIZE - ICON_GAP - TRACKED_EYE_SIZE - ICON_GAP;
    private static final ResourceLocation PAGE_FORWARD_SPRITE = BeastsAndBountiesMod.id("bestiary/page_forward");
    private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED_SPRITE = BeastsAndBountiesMod.id("bestiary/page_forward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD_SPRITE = BeastsAndBountiesMod.id("bestiary/page_backward");
    private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED_SPRITE = BeastsAndBountiesMod.id("bestiary/page_backward_highlighted");

    private final List<Row> discoveredRows = new ArrayList<>();
    private final List<Row> filteredRows = new ArrayList<>();
    private final List<TabSlot> visibleTabs = new ArrayList<>();
    private FormattedCharSequence headerNameLine = FormattedCharSequence.EMPTY;
    private boolean trackButtonShown;
    private Component trackButtonLabel = Component.empty();
    private int trackButtonWidth;
    private final List<FormattedCharSequence> pageLines = new ArrayList<>();
    private EditBox searchBox;
    private int left;
    private int top;
    private float bodyTextScale = 1;
    private int listPage;
    @Nullable
    private ResourceLocation pinnedBounty;
    @Nullable
    private ResourceLocation pinnedTracked;
    private int contentPage;
    private Tab selectedTab = Tab.OVERVIEW;
    @Nullable
    private Row selectedRow;
    @Nullable
    private LivingEntity displayedEntity;

    private BestiaryScreen() {
        super(TITLE);
        if (ClientTracking.trackedCreature().isPresent()) selectedTab = Tab.TRACKING;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new BestiaryScreen());
    }

    @Override
    protected void init() {
        left = (width - SPREAD_WIDTH) / 2;
        top = (height - SPREAD_HEIGHT + TAB_HEIGHT - SELECTED_TAB_OVERLAP) / 2;
        bodyTextScale = GuiText.pixelSnappedBodyScale();
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

    public static void refreshIfOpen() {
        if (Minecraft.getInstance().screen instanceof BestiaryScreen screen) screen.refresh();
    }

    private void refresh() {
        discoveredRows.clear();
        ClientBestiary.entries().forEach((entityId, entry) -> {
            Progress progress = ClientBestiary.progress(entityId);
            if (!entry.isDiscovered(progress)) return;
            Component name = ClientBestiary.displayName(entityId, entry, progress);
            String searchableName = entry.knowledge(progress).name() ? name.getString().toLowerCase(Locale.ROOT) : "";
            discoveredRows.add(new Row(entityId, entry, progress, name, font.split(name, ROW_NAME_WIDTH).getFirst(), searchableName));
        });
        ResourceLocation bountied = ClientBounties.activeBountyCreature().orElse(null);
        ResourceLocation tracked = ClientTracking.trackedCreature().orElse(null);
        if (!Objects.equals(bountied, pinnedBounty) || !Objects.equals(tracked, pinnedTracked)) listPage = 0;
        pinnedBounty = bountied;
        pinnedTracked = tracked;
        discoveredRows.sort(Comparator.comparing((Row row) -> !row.entityId().equals(bountied))
                .thenComparing(row -> !row.entityId().equals(tracked))
                .thenComparing(row -> row.searchableName().isEmpty())
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
        trackButtonShown = false;
        if (selectedRow == null) return;

        Knowledge knowledge = selectedRow.entry().knowledge(selectedRow.progress());
        List<Reward> rewards = selectedRow.entry().rewardsEarned(selectedRow.progress()).toList();
        Bonuses bonuses = selectedRow.entry().bonuses(selectedRow.progress());
        List<Tab> tabs = new ArrayList<>(List.of(Tab.OVERVIEW));
        if (knowledge.hasDetails()) tabs.add(Tab.KNOWLEDGE);
        if (!rewards.isEmpty()) tabs.add(Tab.REWARDS);
        if (bonuses.hasTracking() || isSelectedBountied() || selectedRow.progress().bounties() > 0 || selectedRow.entry().usesBounties()) tabs.add(Tab.TRACKING);
        for (int i = 0; i < tabs.size(); i++) visibleTabs.add(new TabSlot(tabs.get(i), TabShape.at(i, tabs.size())));
        if (!tabs.contains(selectedTab)) selectedTab = Tab.OVERVIEW;
        rebuildHeader();

        switch (selectedTab) {
            case OVERVIEW -> buildOverview(knowledge);
            case KNOWLEDGE -> buildKnowledge(knowledge);
            case REWARDS -> buildRewards(rewards);
            case TRACKING -> buildTracking(bonuses);
        }

        contentPage = Math.min(contentPage, contentPageCount() - 1);
        if (displayedEntity == null && knowledge.model()) displayedEntity = createDisplayEntity(selectedRow.entityId());
    }

    private boolean isSelectedBountied() {
        return ClientBounties.activeBountyCreature().filter(selectedRow.entityId()::equals).isPresent();
    }

    private void rebuildHeader() {
        trackButtonShown = selectedRow.entry().bonuses(selectedRow.progress()).hasTracking();
        trackButtonLabel = Component.translatable(isSelectedTracked() ? "gui.beasts_and_bounties.stop_tracking" : "gui.beasts_and_bounties.track");
        trackButtonWidth = font.width(trackButtonLabel) + TRACK_BUTTON_PADDING * 2;
        headerNameLine = font.split(selectedRow.name(), HEADER_AREA.width() - ICON_SIZE - ICON_GAP).getFirst();
    }

    private void buildTracking(Bonuses bonuses) {
        ClientBounties.activeBounty().filter(bounty -> bounty.entityId().equals(selectedRow.entityId())).ifPresent(bounty -> {
            addLine(Component.translatable("gui.beasts_and_bounties.bounty_progress", bounty.killsSoFar(), bounty.killsRequired()).withStyle(ChatFormatting.DARK_RED));
            pageLines.add(FormattedCharSequence.EMPTY);
        });
        if (!bonuses.hasTracking()) return;
        addLine(Component.translatable(isSelectedTracked() ? "gui.beasts_and_bounties.tracking.tracked" : "gui.beasts_and_bounties.tracking.untracked"));
        pageLines.add(FormattedCharSequence.EMPTY);
        Component name = selectedRow.entry().displayName(selectedRow.entityId(), selectedRow.progress());
        if (bonuses.passiveTracking() == 0) addLine(Component.translatable("gui.beasts_and_bounties.tracking.passive.none"));
        else addTrackingLevel("passive", bonuses.passiveTracking(), name);
        pageLines.add(FormattedCharSequence.EMPTY);
        if (bonuses.activeTracking() == 0) addLine(Component.translatable("gui.beasts_and_bounties.tracking.active.none"));
        else addTrackingLevel("active", bonuses.activeTracking(), TrackingClientEvents.huntKeyName(), name);
    }

    private void addTrackingLevel(String mode, int level, Object... details) {
        String prefix = "tracking.beasts_and_bounties." + mode + "." + level;
        addLine(Component.translatable("gui.beasts_and_bounties.tracking." + mode + ".heading",
                Component.translatable("enchantment.level." + level), Component.translatable(prefix + ".name")).withStyle(ChatFormatting.BOLD));
        addLine(Component.translatable(prefix + ".description", details));
    }

    private void buildOverview(Knowledge knowledge) {
        BestiaryEntry entry = selectedRow.entry();
        if (entry.classification() != Classification.NORMAL)
            addLine(Component.translatable("classification.beasts_and_bounties." + entry.classification().getSerializedName())
                    .withStyle(entry.classification().nameColor()));
        knowledge.type().ifPresent(type -> addLine(type.copy().withStyle(ChatFormatting.ITALIC)));
        addLine(Component.translatable("gui.beasts_and_bounties.kills", selectedRow.progress().kills()));
        addLine(Component.translatable("gui.beasts_and_bounties.bounties", selectedRow.progress().bounties()));
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
            TabSlot slot = visibleTabs.get(i);
            boolean selected = slot.tab() == selectedTab;
            if (selected != selectedPass) continue;
            int tabX = tabX(i);
            int tabY = tabY(selected);
            graphics.blitSprite(selected ? slot.shape().selectedSprite : slot.shape().sprite, tabX, tabY, TAB_WIDTH, TAB_HEIGHT);
            graphics.blitSprite(slot.tab().iconSprite, tabX + TAB_ICON_INSET_X, tabY + TAB_ICON_INSET_Y, TAB_ICON_SIZE, TAB_ICON_SIZE);
        }
    }

    private void renderTabTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Tab hovered = tabAt(mouseX, mouseY);
        if (hovered != null) Tooltips.render(graphics, font, List.of(hovered.title), mouseX, mouseY);
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
            boolean bountied = row.entityId().equals(pinnedBounty);
            boolean tracked = row.entityId().equals(pinnedTracked);
            if (bountied) graphics.fill(x - 1, rowY, x + LIST_AREA.width(), rowY + ROW_HEIGHT, BOUNTY_ROW_COLOR);
            else if (tracked) graphics.fill(x - 1, rowY, x + LIST_AREA.width(), rowY + ROW_HEIGHT, TRACKED_ROW_COLOR);
            if (row == selectedRow) graphics.fill(x - 1, rowY, x + LIST_AREA.width(), rowY + ROW_HEIGHT, SELECTED_ROW_COLOR);
            else if (row == hovered) graphics.fill(x - 1, rowY, x + LIST_AREA.width(), rowY + ROW_HEIGHT, HOVERED_ROW_COLOR);
            renderNameWithIcon(graphics, row.entry().classification(), row.nameLine(), x, rowY + 2);
            if (bountied || tracked) graphics.blitSprite(bountied && tracked ? TRACKED_BOUNTY_MARK_SPRITE : bountied ? BOUNTY_MARK_SPRITE : TRACKED_EYE_SPRITE,
                    x + LIST_AREA.width() - TRACKED_EYE_SIZE, rowY + (ROW_HEIGHT - TRACKED_EYE_SIZE) / 2, TRACKED_EYE_SIZE, TRACKED_EYE_SIZE);
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

    private boolean isSelectedTracked() {
        return selectedRow != null && ClientTracking.trackedCreature().filter(selectedRow.entityId()::equals).isPresent();
    }

    private int trackButtonX() {
        return left + CONTENT_AREA.x();
    }

    private int trackButtonY() {
        return top + CONTENT_AREA.y();
    }

    private boolean isOverTrackButton(double mouseX, double mouseY) {
        return selectedTab == Tab.TRACKING && trackButtonShown && mouseX >= trackButtonX() && mouseX < trackButtonX() + trackButtonWidth
                && mouseY >= trackButtonY() && mouseY < trackButtonY() + TRACK_BUTTON_HEIGHT;
    }

    private void renderTrackButton(GuiGraphics graphics, int mouseX, int mouseY) {
        ResourceLocation sprite = isOverTrackButton(mouseX, mouseY) ? TRACK_BUTTON_HIGHLIGHTED_SPRITE : TRACK_BUTTON_SPRITE;
        graphics.blitSprite(sprite, trackButtonX(), trackButtonY(), trackButtonWidth, TRACK_BUTTON_HEIGHT);
        graphics.drawString(font, trackButtonLabel, trackButtonX() + TRACK_BUTTON_PADDING,
                trackButtonY() + (TRACK_BUTTON_HEIGHT - GLYPH_HEIGHT) / 2, TRACK_BUTTON_TEXT_COLOR, true);
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
        boolean highlighted = isOverArrow(mouseX, mouseY, footer, forward);
        ResourceLocation sprite = forward
                ? (highlighted ? PAGE_FORWARD_HIGHLIGHTED_SPRITE : PAGE_FORWARD_SPRITE)
                : (highlighted ? PAGE_BACKWARD_HIGHLIGHTED_SPRITE : PAGE_BACKWARD_SPRITE);
        graphics.blitSprite(sprite, arrowX(footer, forward), top + footer.y(), ARROW_WIDTH, ARROW_HEIGHT);
    }

    private void renderNameWithIcon(GuiGraphics graphics, Classification classification, FormattedCharSequence nameLine, int x, int y) {
        classification.iconSprite().ifPresent(sprite -> graphics.blitSprite(sprite, x, y, ICON_SIZE, ICON_SIZE));
        graphics.drawString(font, nameLine, x + ICON_SIZE + ICON_GAP, y, TEXT_COLOR, false);
    }

    private void renderPage(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + CONTENT_AREA.x();
        int headerX = left + HEADER_AREA.x();
        int headerY = titleTextY(HEADER_AREA);
        if (selectedRow == null) {
            List<FormattedCharSequence> emptyLines = font.split(Component.translatable("gui.beasts_and_bounties.empty"), CONTENT_AREA.width());
            for (int i = 0; i < emptyLines.size(); i++)
                graphics.drawString(font, emptyLines.get(i), x, top + CONTENT_AREA.y() + i * LINE_HEIGHT, MUTED_TEXT_COLOR, false);
            return;
        }
        renderNameWithIcon(graphics, selectedRow.entry().classification(), headerNameLine, headerX, headerY);
        if (selectedTab == Tab.OVERVIEW) renderModel(graphics, x, top + CONTENT_AREA.y());
        if (selectedTab == Tab.TRACKING && trackButtonShown) renderTrackButton(graphics, mouseX, mouseY);
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
        return CONTENT_AREA.y() + switch (selectedTab) {
            case OVERVIEW -> MODEL_BOX_HEIGHT;
            case TRACKING -> trackButtonShown ? TRACK_BUTTON_HEIGHT + TRACK_BUTTON_GAP : 0;
            default -> 0;
        };
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
            if (selectedRow == null || !row.entityId().equals(selectedRow.entityId())) selectedTab = Tab.OVERVIEW;
            select(row);
            return true;
        }
        if (turnPageIfArrowClicked(mouseX, mouseY)) return true;
        if (isOverTrackButton(mouseX, mouseY)) {
            ClientTracking.requestTracking(isSelectedTracked() ? Optional.empty() : Optional.of(selectedRow.entityId()));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
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
            Tab tab = visibleTabs.get(i).tab();
            int tabY = tabY(tab == selectedTab);
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH && mouseY >= tabY && mouseY < top) return tab;
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


    private static int guiPixels(int texturePixels) {
        return Math.round(texturePixels / TEXTURE_PIXELS_PER_GUI_PIXEL);
    }
}
