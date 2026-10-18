package net.silvertide.beasts_and_bounties.feature.combat;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SupportLog {
    public static final Codec<SupportLog> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).xmap(SupportLog::new, log -> log.lastSupportTicks);

    private final Map<UUID, Long> lastSupportTicks;

    SupportLog() {
        this(Map.of());
    }

    private SupportLog(Map<UUID, Long> lastSupportTicks) {
        this.lastSupportTicks = new HashMap<>(lastSupportTicks);
    }

    Set<UUID> supportersSince(long earliestTick) {
        Set<UUID> recent = new HashSet<>();
        lastSupportTicks.forEach((supporterId, tick) -> {
            if (tick >= earliestTick) recent.add(supporterId);
        });
        return recent;
    }

    SupportLog withEffect(UUID supporterId, long gameTick) {
        SupportLog copy = new SupportLog(lastSupportTicks);
        copy.lastSupportTicks.merge(supporterId, gameTick, Math::max);
        return copy;
    }
}
