package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.List;

public final class BountyBoardData extends SavedData {
    private static final String REROLLS = "rerolls";
    private static final String PERIOD = "period";
    private static final String PERIOD_REROLLS = "period_rerolls";
    private static final String OFFERS = "offers";

    private int rerolls;
    private long period = -1;
    private int periodRerolls = -1;
    private List<ResourceLocation> offers = List.of();

    private BountyBoardData() {}

    private static BountyBoardData load(CompoundTag tag) {
        BountyBoardData data = new BountyBoardData();
        data.rerolls = tag.getInt(REROLLS);
        data.period = tag.getLong(PERIOD);
        data.periodRerolls = tag.getInt(PERIOD_REROLLS);
        data.offers = tag.getList(OFFERS, Tag.TAG_STRING).stream().map(entry -> new ResourceLocation(entry.getAsString())).toList();
        return data;
    }

    public static BountyBoardData of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BountyBoardData::load, BountyBoardData::new, BeastsAndBountiesMod.MODID + "_board");
    }

    public int rerolls() {
        return rerolls;
    }

    void reroll() {
        rerolls++;
        setDirty();
    }

    boolean isCurrent(long now) {
        return period == now && periodRerolls == rerolls;
    }

    List<ResourceLocation> offers() {
        return offers;
    }

    void record(long now, List<ResourceLocation> newOffers) {
        period = now;
        periodRerolls = rerolls;
        offers = List.copyOf(newOffers);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(REROLLS, rerolls);
        tag.putLong(PERIOD, period);
        tag.putInt(PERIOD_REROLLS, periodRerolls);
        ListTag list = new ListTag();
        offers.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        tag.put(OFFERS, list);
        return tag;
    }
}
