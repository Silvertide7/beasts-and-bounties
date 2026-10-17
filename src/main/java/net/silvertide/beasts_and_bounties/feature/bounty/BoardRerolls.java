package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

public final class BoardRerolls extends SavedData {
    private static final String KEY = "rerolls";
    private static final Factory<BoardRerolls> FACTORY = new Factory<>(BoardRerolls::new, BoardRerolls::load);

    private int count;

    private BoardRerolls() {}

    private static BoardRerolls load(CompoundTag tag, HolderLookup.Provider registries) {
        BoardRerolls rerolls = new BoardRerolls();
        rerolls.count = tag.getInt(KEY);
        return rerolls;
    }

    static BoardRerolls of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, BeastsAndBountiesMod.MODID + "_board_rerolls");
    }

    public int count() {
        return count;
    }

    void increment() {
        count++;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(KEY, count);
        return tag;
    }
}
