package net.silvertide.beasts_and_bounties.feature.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;
import net.silvertide.beasts_and_bounties.core.PlayerData;
import net.silvertide.beasts_and_bounties.core.PlayerDataEvents;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CombatTracking {
    private static final String LOG_KEY = BeastsAndBountiesMod.MODID + ":combat_log";
    private static final PlayerData<SupportLog> SUPPORT = PlayerDataEvents.register(new PlayerData<>("combat_support", SupportLog.CODEC));

    private CombatTracking() {}

    public static Set<UUID> participants(LivingEntity victim, long windowTicks) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) return Set.of();
        long earliestTick = serverLevel.getGameTime() - windowTicks;
        Set<UUID> damagerIds = logOf(victim).contributorsSince(earliestTick);
        if (damagerIds.isEmpty()) return Set.of();

        Set<UUID> participantIds = new HashSet<>(damagerIds);
        PlayerList playerList = serverLevel.getServer().getPlayerList();
        for (UUID damagerId : damagerIds) {
            ServerPlayer damager = playerList.getPlayer(damagerId);
            if (damager != null) SUPPORT.get(damager).ifPresent(support -> participantIds.addAll(support.supportersSince(earliestTick)));
        }
        return participantIds;
    }

    @Nullable
    public static ServerPlayer attributedPlayer(@Nullable Entity entity) {
        if (entity == null || entity instanceof FakePlayer) return null;
        if (entity instanceof ServerPlayer player) return player;
        Entity owner = declaredOwnerOf(entity);
        return owner instanceof ServerPlayer player && !(owner instanceof FakePlayer) ? player : null;
    }

    static void recordDamage(LivingEntity victim, @Nullable Entity attacker, float dealt) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer contributor = attributedPlayer(attacker);
        long gameTick = serverLevel.getGameTime();
        updateLog(victim, log -> {
            if (contributor != null && contributor != victim) log.recordDirectDamage(contributor.getUUID(), dealt, gameTick);
            else if (attacker == null) log.recordSourcelessDamage(dealt, gameTick);
            else log.recordNonPlayerDamage(dealt);
        });
    }

    static void recordEffect(LivingEntity target, @Nullable Entity applier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer contributor = attributedPlayer(applier);
        if (contributor == null || contributor == target) return;
        long gameTick = serverLevel.getGameTime();
        if (target instanceof Player supported) {
            SUPPORT.set(supported, SUPPORT.get(supported).orElseGet(SupportLog::new).withEffect(contributor.getUUID(), gameTick));
        } else {
            updateLog(target, log -> log.recordEffect(contributor.getUUID(), gameTick));
        }
    }

    static void clearLog(LivingEntity entity) {
        entity.getPersistentData().remove(LOG_KEY);
    }

    static boolean hasLog(LivingEntity entity) {
        return entity.getPersistentData().contains(LOG_KEY, Tag.TAG_COMPOUND);
    }

    private static CombatLog logOf(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(LOG_KEY, Tag.TAG_COMPOUND)) return new CombatLog();
        return CombatLog.CODEC.parse(NbtOps.INSTANCE, data.getCompound(LOG_KEY)).result().orElseGet(CombatLog::new);
    }

    private static void updateLog(LivingEntity entity, java.util.function.Consumer<CombatLog> change) {
        CombatLog log = logOf(entity);
        change.accept(log);
        if (log.isEmpty()) clearLog(entity);
        else entity.getPersistentData().put(LOG_KEY, CombatLog.CODEC.encodeStart(NbtOps.INSTANCE, log).getOrThrow(false, error -> {}));
    }

    @Nullable
    private static Entity declaredOwnerOf(Entity entity) {
        if (entity instanceof OwnableEntity ownable) return ownable.getOwner();
        if (entity instanceof TraceableEntity traceable) return traceable.getOwner();
        return null;
    }
}
