package net.silvertide.beasts_and_bounties.feature.tracking;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TrackingClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue WISP_BLOCKS_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue WISP_MAX_LENGTH_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue WISP_STOP_SHORT_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue WISP_SWIRL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue WISP_SWIRL_RADIANS_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue PING_TEXT_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("The wisp trail that level 3 passive tracking sends toward the creature.").push("wisp");
        WISP_BLOCKS_PER_TICK = builder.comment("How fast the wisp travels, in blocks per tick (20 ticks = 1 second).")
                .defineInRange("blocksPerTick", 0.45, 0.05, 5.0);
        WISP_MAX_LENGTH_BLOCKS = builder.comment("The furthest the wisp travels, in blocks.")
                .defineInRange("maxLengthBlocks", 24.0, 1.0, 128.0);
        WISP_STOP_SHORT_BLOCKS = builder.comment("The wisp always stops at least this many blocks before the creature.")
                .defineInRange("stopShortBlocks", 8.0, 0.0, 64.0);
        WISP_SWIRL_RADIUS = builder.comment("How wide the wisp's spiral is, in blocks. 0 flies straight.")
                .defineInRange("swirlRadius", 0.25, 0.0, 2.0);
        WISP_SWIRL_RADIANS_PER_TICK = builder.comment("How fast the wisp spirals, in radians per tick.")
                .defineInRange("swirlRadiansPerTick", 0.4, 0.0, 3.2);
        builder.pop();
        builder.push("hud");
        PING_TEXT_SECONDS = builder.comment("How long a passive ping stays fully readable, in seconds, before it fades out.")
                .defineInRange("pingTextSeconds", 4.0, 0.5, 30.0);
        builder.pop();
        SPEC = builder.build();
    }

    private TrackingClientConfig() {}
}
