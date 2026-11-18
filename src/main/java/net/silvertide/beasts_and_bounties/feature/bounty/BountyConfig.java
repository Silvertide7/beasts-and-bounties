package net.silvertide.beasts_and_bounties.feature.bounty;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BountyConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue REFRESH_DAYS;
    static final ModConfigSpec.IntValue OFFERS_SHOWN;
    public static final ModConfigSpec.BooleanValue ALLOW_ABANDON;
    private static final ModConfigSpec.BooleanValue SHOW_LAST_SEEN;
    static final ModConfigSpec.BooleanValue BALANCED_POSTINGS;
    static final ModConfigSpec.BooleanValue PER_BOARD_POSTINGS;


    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("The bounty board: postings shared by the whole server that change at dawn. Each player can claim each posting once.").push("board");
        REFRESH_DAYS = builder.comment("How many dawns pass between new postings. It follows the world's day/night cycle, so mods that lengthen the day lengthen it too, and sleeping through the night brings the next dawn.")
                .defineInRange("refreshDays", 1, 1, 100);
        OFFERS_SHOWN = builder.comment("How many postings the board shows at once.")
                .defineInRange("offersShown", 3, 1, 3);
        ALLOW_ABANDON = builder.comment("Whether a player may abandon an accepted bounty.")
                .define("allowAbandon", true);
        SHOW_LAST_SEEN = builder.comment("Whether hovering a posting shows where the creature was last seen (its habitat), even before the player has learned it.")
                .define("showLastSeen", false);
        BALANCED_POSTINGS = builder.comment("Whether every board holds at least one elite or boss posting and one normal posting, with the rest rolled freely by weight. When false every posting is a free weighted roll.")
                .define("balancedPostings", false);
        PER_BOARD_POSTINGS = builder.comment("Whether each board rolls its own postings from where it stands, so two boards side by side offer different creatures. Breaking a board and placing it back in the same spot gives the same postings. A creature claimed on one board counts as claimed on every board, and a player still holds one bounty at a time. When false every board shows the same postings.")
                .define("perBoardPostings", false);
        builder.pop();
        SPEC = builder.build();
    }

    private BountyConfig() {}

    static boolean showLastSeen() {
        return SHOW_LAST_SEEN.get();
    }

    static int refreshDays() {
        return REFRESH_DAYS.get();
    }
}
