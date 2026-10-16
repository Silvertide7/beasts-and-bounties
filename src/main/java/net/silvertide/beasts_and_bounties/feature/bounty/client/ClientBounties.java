package net.silvertide.beasts_and_bounties.feature.bounty.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.BestiaryScreen;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.BestiaryToast;
import net.silvertide.beasts_and_bounties.feature.bestiary.client.ClientBestiary;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyPayloads;
import net.silvertide.beasts_and_bounties.feature.bounty.BountyState;

import java.util.Optional;

public final class ClientBounties {
    private static BountyState state = BountyState.NONE;

    private ClientBounties() {}

    public static void setState(BountyState newState) {
        state = newState;
        BestiaryScreen.refreshIfOpen();
    }

    public static void openBoard(BountyPayloads.BoardOpened board) {
        state = board.state();
        Minecraft.getInstance().setScreen(new BountyBoardScreen(board));
    }

    public static void claimed(ResourceLocation entityId) {
        BestiaryToast.show(Component.translatable("toast.beasts_and_bounties.bounty_claimed"), ClientBestiary.creatureName(entityId));
    }

    public static Optional<BountyState.ActiveBounty> activeBounty() {
        return state.active();
    }

    public static Optional<ResourceLocation> activeBountyCreature() {
        return state.active().map(BountyState.ActiveBounty::entityId);
    }
}
