package net.silvertide.beasts_and_bounties.feature.bounty;

import net.minecraft.SharedConstants;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BountyConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue REFRESH_MINECRAFT_DAYS;
    static final ModConfigSpec.IntValue OFFERS_SHOWN;
    private static final ModConfigSpec.IntValue CLAIM_COOLDOWN_MINUTES;
    private static final ModConfigSpec.IntValue ABANDON_COOLDOWN_MINUTES;
    public static final ModConfigSpec.BooleanValue ALLOW_ABANDON;
    private static final ModConfigSpec.BooleanValue SHOW_LAST_SEEN;

    private static final int TICKS_PER_MINECRAFT_DAY = 24000;
    private static final int SECONDS_PER_MINUTE = 60;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("The bounty board: per-player postings that refresh on a timer.").push("board");
        REFRESH_MINECRAFT_DAYS = builder.comment("How often the postings change, in Minecraft days (1.0 = one day/night cycle).")
                .defineInRange("refreshMinecraftDays", 1.0, 0.05, 100.0);
        OFFERS_SHOWN = builder.comment("How many postings the board shows at once.")
                .defineInRange("offersShown", 3, 1, 3);
        CLAIM_COOLDOWN_MINUTES = builder.comment("Real minutes after claiming a bounty before the board offers another.")
                .defineInRange("claimCooldownMinutes", 10, 0, 1440);
        ABANDON_COOLDOWN_MINUTES = builder.comment("Real minutes after abandoning a bounty before the board offers another.")
                .defineInRange("abandonCooldownMinutes", 20, 0, 1440);
        ALLOW_ABANDON = builder.comment("Whether a player may abandon an accepted bounty.")
                .define("allowAbandon", true);
        SHOW_LAST_SEEN = builder.comment("Whether hovering a posting shows where the creature was last seen (its habitat), even before the player has learned it.")
                .define("showLastSeen", false);
        builder.pop();
        SPEC = builder.build();
    }

    private BountyConfig() {}

    static boolean showLastSeen() {
        return SHOW_LAST_SEEN.get();
    }

    static long refreshTicks() {
        return Math.max(1, Math.round(REFRESH_MINECRAFT_DAYS.get() * TICKS_PER_MINECRAFT_DAY));
    }

    static long claimCooldownTicks() {
        return (long) CLAIM_COOLDOWN_MINUTES.get() * SECONDS_PER_MINUTE * SharedConstants.TICKS_PER_SECOND;
    }

    static long abandonCooldownTicks() {
        return (long) ABANDON_COOLDOWN_MINUTES.get() * SECONDS_PER_MINUTE * SharedConstants.TICKS_PER_SECOND;
    }
}
