package net.silvertide.beasts_and_bounties.feature.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CombatLog {
    private static final long INDIRECT_ATTRIBUTION_TICKS = 100L;

    public static final Codec<CombatLog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("total_damage_taken").forGetter(log -> log.totalDamageTaken),
            UUIDUtil.STRING_CODEC.optionalFieldOf("last_direct_contributor").forGetter(log -> Optional.ofNullable(log.lastDirectContributor)),
            Codec.LONG.optionalFieldOf("last_direct_tick", 0L).forGetter(log -> log.lastDirectTick),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).fieldOf("contributions").forGetter(log -> log.lastContributionTicks)
    ).apply(instance, CombatLog::new));

    private final Map<UUID, Long> lastContributionTicks;
    private float totalDamageTaken;
    private UUID lastDirectContributor;
    private long lastDirectTick;

    CombatLog() {
        this(0.0f, Optional.empty(), 0L, Map.of());
    }

    private CombatLog(float totalDamageTaken, Optional<UUID> lastDirectContributor, long lastDirectTick, Map<UUID, Long> lastContributionTicks) {
        this.totalDamageTaken = totalDamageTaken;
        this.lastDirectContributor = lastDirectContributor.orElse(null);
        this.lastDirectTick = lastDirectTick;
        this.lastContributionTicks = new HashMap<>(lastContributionTicks);
    }

    Set<UUID> contributorsSince(long earliestTick) {
        Set<UUID> recent = new HashSet<>();
        lastContributionTicks.forEach((playerId, tick) -> {
            if (tick >= earliestTick) recent.add(playerId);
        });
        return recent;
    }

    boolean isEmpty() {
        return totalDamageTaken <= 0.0f && lastContributionTicks.isEmpty();
    }

    void clear() {
        lastContributionTicks.clear();
        totalDamageTaken = 0.0f;
        lastDirectContributor = null;
        lastDirectTick = 0L;
    }

    void recordDirectDamage(UUID playerId, float dealt, long gameTick) {
        totalDamageTaken += dealt;
        lastDirectContributor = playerId;
        lastDirectTick = gameTick;
        lastContributionTicks.merge(playerId, gameTick, Math::max);
    }

    void recordSourcelessDamage(float dealt, long gameTick) {
        totalDamageTaken += dealt;
        if (lastDirectContributor == null || gameTick - lastDirectTick > INDIRECT_ATTRIBUTION_TICKS) return;
        lastContributionTicks.merge(lastDirectContributor, gameTick, Math::max);
    }

    void recordNonPlayerDamage(float dealt) {
        totalDamageTaken += dealt;
    }

    void recordEffect(UUID playerId, long gameTick) {
        lastContributionTicks.merge(playerId, gameTick, Math::max);
    }
}
