package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.silvertide.beasts_and_bounties.core.Network;
import net.silvertide.beasts_and_bounties.core.PlayerData;
import net.silvertide.beasts_and_bounties.core.PlayerDataEvents;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;

import java.util.List;
import java.util.Optional;

public final class PlayerBounties {
    private static final float CLAIMED_VOLUME = 0.6f;

    private static final PlayerData<BountyState> STATE = PlayerDataEvents.register(new PlayerData<>("bounty", BountyState.CODEC));

    private PlayerBounties() {}

    public static BountyState state(Player player) {
        return STATE.get(player).orElse(BountyState.NONE);
    }

    public static void setState(Player player, BountyState state) {
        STATE.set(player, state);
    }

    static void openBoard(ServerPlayer player) {
        sendBoard(player, false);
    }

    private static void sendBoard(ServerPlayer player, boolean refreshOnly) {
        long now = player.level().getGameTime();
        BountyState state = state(player);
        List<BountyOffers.Offer> shown = state.active()
                .map(bounty -> Optional.ofNullable(BountyDefinitions.all().get(bounty.entityId()))
                        .map(definition -> List.of(new BountyOffers.Offer(bounty.entityId(), definition))).orElse(List.of()))
                .orElseGet(() -> BountyOffers.current(player.server, now));
        List<BountyPayloads.Offer> offers = shown.stream()
                .map(offer -> new BountyPayloads.Offer(offer.entityId(), offer.definition().kills(), offer.definition().itemRewards(),
                        offer.definition().experienceReward(), skillExperienceLines(offer.definition()), lastSeen(offer.entityId())))
                .toList();
        Network.sendIfConnected(player, new BountyPayloads.BoardOpened(offers, BountyOffers.periodEndsAt(now), state, refreshOnly));
    }

    private static List<Component> skillExperienceLines(BountyDefinition definition) {
        if (!PmmoBridge.isLoaded()) return List.of();
        return definition.skillExperienceRewards().stream()
                .map(reward -> (Component) Component.translatable("gui.beasts_and_bounties.board.skill_experience", reward.amount(), Component.translatable("pmmo." + reward.skill())))
                .toList();
    }

    private static List<Component> lastSeen(ResourceLocation entityId) {
        BestiaryEntry entry = BestiaryEntries.get(entityId);
        return BountyConfig.showLastSeen() && entry != null ? entry.fullKnowledge().habitat() : List.of();
    }

    public static void accept(ServerPlayer player, ResourceLocation entityId) {
        long now = player.level().getGameTime();
        BountyState state = state(player);
        if (!state.canAccept(now)) {
            player.displayClientMessage(Component.translatable(state.active().isPresent()
                    ? "bounty.beasts_and_bounties.already_active" : "bounty.beasts_and_bounties.cooldown"), true);
            return;
        }
        Optional<BountyOffers.Offer> offer = BountyOffers.current(player.server, now).stream().filter(candidate -> candidate.entityId().equals(entityId)).findFirst();
        if (offer.isEmpty()) {
            player.displayClientMessage(Component.translatable("bounty.beasts_and_bounties.not_offered"), true);
            return;
        }
        int kills = offer.get().definition().kills();
        store(player, state.accepted(entityId, kills));
        player.displayClientMessage(Component.translatable("bounty.beasts_and_bounties.accepted" + (kills == 1 ? ".one" : ""), creatureName(entityId), kills), true);
        sendBoard(player, true);
    }

    public static void abandon(ServerPlayer player) {
        if (!BountyConfig.ALLOW_ABANDON.get() || state(player).active().isEmpty()) return;
        store(player, state(player).ended(player.level().getGameTime(), BountyConfig.abandonCooldownTicks()));
        player.displayClientMessage(Component.translatable("bounty.beasts_and_bounties.abandoned"), true);
        sendBoard(player, true);
    }

    static void recordKill(ServerPlayer player, ResourceLocation entityId) {
        state(player).killRecorded(entityId).ifPresent(progressed -> {
            BountyState.ActiveBounty bounty = progressed.active().orElseThrow();
            if (bounty.isComplete()) {
                complete(player, bounty);
                return;
            }
            store(player, progressed);
            player.displayClientMessage(Component.translatable("bounty.beasts_and_bounties.progress", bounty.killsSoFar(), bounty.killsRequired()), true);
        });
    }

    private static void complete(ServerPlayer player, BountyState.ActiveBounty bounty) {
        BountyDefinition definition = BountyDefinitions.all().get(bounty.entityId());
        if (definition != null) {
            definition.itemRewards().forEach(stack -> ItemHandlerHelper.giveItemToPlayer(player, stack.copy()));
            if (definition.experienceReward() > 0) player.giveExperiencePoints(definition.experienceReward());
            if (PmmoBridge.isLoaded()) definition.skillExperienceRewards().forEach(reward -> PmmoBridge.addSkillExperience(player, reward));
        }
        BestiaryEntry entry = BestiaryEntries.get(bounty.entityId());
        if (entry != null) {
            BestiaryProgress.addBounties(player, bounty.entityId(), entry, 1);
            if (definition != null && definition.bonusKills() > 0) BestiaryProgress.addKills(player, bounty.entityId(), entry, definition.bonusKills());
        }
        store(player, state(player).ended(player.level().getGameTime(), BountyConfig.claimCooldownTicks()));
        player.playNotifySound(BountySounds.CLAIMED.get(), SoundSource.PLAYERS, CLAIMED_VOLUME, 1f);
        Network.sendIfConnected(player, new BountyPayloads.BountyClaimed(bounty.entityId()));
    }

    static void reroll(MinecraftServer server) {
        BountyBoardData.of(server).reroll();
    }

    static void clear(ServerPlayer player) {
        store(player, state(player).cleared());
    }

    private static void store(ServerPlayer player, BountyState state) {
        if (state.isEmpty(player.level().getGameTime())) STATE.remove(player);
        else STATE.set(player, state);
        Network.sendIfConnected(player, new BountyPayloads.StateSync(state(player)));
    }

    private static Component creatureName(ResourceLocation entityId) {
        return ForgeRegistries.ENTITY_TYPES.getValue(entityId).getDescription();
    }
}
