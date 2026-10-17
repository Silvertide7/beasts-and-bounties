package net.silvertide.beasts_and_bounties.feature.bounty;

import harmonised.pmmo.core.Core;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

final class PmmoBridge {
    private static final boolean LOADED = ModList.get().isLoaded("pmmo");

    private PmmoBridge() {}

    static boolean isLoaded() {
        return LOADED;
    }

    static void addSkillExperience(ServerPlayer player, BountyReward.SkillExperience reward) {
        Core.get(player.level()).getData().addXp(player.getUUID(), reward.skill(), reward.amount());
    }
}
