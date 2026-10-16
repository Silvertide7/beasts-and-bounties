package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.Payloads;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntries;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryEntry;
import net.silvertide.beasts_and_bounties.feature.bestiary.BestiaryProgress;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class PlayerBounties {
    private static final float CLAIMED_VOLUME = 0.6f;

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BeastsAndBountiesMod.MODID);

    static final Supplier<AttachmentType<BountyState>> STATE =
            ATTACHMENT_TYPES.register("bounty",
                    () -> AttachmentType.builder(() -> BountyState.NONE)
                            .serialize(BountyState.CODEC)
                            .copyOnDeath()
                            .build());

    private PlayerBounties() {}

    static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static BountyState state(Player player) {
        BountyState state = player.getExistingDataOrNull(STATE);
        return state == null ? BountyState.NONE : state;
    }

    static void openBoard(ServerPlayer player) {
        long now = player.level().getGameTime();
        List<BountyPayloads.Offer> offers = BountyOffers.forPlayer(player, now).stream()
                .map(offer -> new BountyPayloads.Offer(offer.entityId(), offer.definition().kills(), offer.definition().itemRewards(),
                        offer.definition().experienceReward(), BestiaryEntries.get(offer.entityId()).fullKnowledge().habitat()))
                .toList();
        Payloads.sendIfConnected(player, new BountyPayloads.BoardOpened(offers, BountyOffers.periodEndsAt(now), state(player)));
    }

    static void accept(ServerPlayer player, ResourceLocation entityId) {
        long now = player.level().getGameTime();
        BountyState state = state(player);
        if (!state.canAccept(now)) {
            player.displayClientMessage(Component.translatable(state.active().isPresent()
                    ? "bounty.beasts_and_bounties.already_active" : "bounty.beasts_and_bounties.cooldown"), true);
            return;
        }
        Optional<BountyOffers.Offer> offer = BountyOffers.forPlayer(player, now).stream().filter(candidate -> candidate.entityId().equals(entityId)).findFirst();
        if (offer.isEmpty()) {
            player.displayClientMessage(Component.translatable("bounty.beasts_and_bounties.not_offered"), true);
            return;
        }
        int kills = offer.get().definition().kills();
        store(player, state.accepted(entityId, kills));
        player.displayClientMessage(Component.translatable("bounty.beasts_and_bounties.accepted" + (kills == 1 ? ".one" : ""), creatureName(player, entityId), kills), true);
    }

    static void abandon(ServerPlayer player) {
        if (!BountyConfig.ALLOW_ABANDON.get() || state(player).active().isEmpty()) return;
        store(player, state(player).ended(player.level().getGameTime(), BountyConfig.abandonCooldownTicks()));
        player.displayClientMessage(Component.translatable("bounty.beasts_and_bounties.abandoned"), true);
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
        }
        BestiaryEntry entry = BestiaryEntries.get(bounty.entityId());
        if (entry != null) {
            BestiaryProgress.addBounties(player, bounty.entityId(), entry, 1);
            if (definition != null && definition.bonusKills() > 0) BestiaryProgress.addKills(player, bounty.entityId(), entry, definition.bonusKills());
        }
        store(player, state(player).ended(player.level().getGameTime(), BountyConfig.claimCooldownTicks()));
        player.playNotifySound(BountySounds.CLAIMED.get(), SoundSource.PLAYERS, CLAIMED_VOLUME, 1f);
        Payloads.sendIfConnected(player, new BountyPayloads.BountyClaimed(bounty.entityId()));
    }

    static void reroll(ServerPlayer player) {
        store(player, state(player).rerolled());
    }

    static void clear(ServerPlayer player) {
        store(player, state(player).cleared());
    }

    private static void store(ServerPlayer player, BountyState state) {
        if (state.isEmpty(player.level().getGameTime())) player.removeData(STATE);
        else player.setData(STATE, state);
        Payloads.sendIfConnected(player, new BountyPayloads.StateSync(state(player)));
    }

    private static Component creatureName(Player player, ResourceLocation entityId) {
        BestiaryEntry entry = BestiaryEntries.get(entityId);
        return entry == null ? Component.literal(entityId.toString()) : entry.displayName(entityId, BestiaryProgress.of(player, entityId));
    }
}
