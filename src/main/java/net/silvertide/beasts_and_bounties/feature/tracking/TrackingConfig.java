package net.silvertide.beasts_and_bounties.feature.tracking;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TrackingConfig {
    public static final ModConfigSpec SPEC;

    static final PassiveLevel[] PASSIVE_LEVELS = new PassiveLevel[PassiveTrackingLevel.values().length];
    static final ActiveLevel[] ACTIVE_LEVELS = new ActiveLevel[ActiveTrackingLevel.values().length];
    static final ModConfigSpec.IntValue GLOW_RANGE;
    static final ModConfigSpec.IntValue HUNT_SCAN_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue HUNT_HOLD_SECONDS;
    static final ModConfigSpec.DoubleValue HEIGHT_HINT_BLOCKS;
    static final ModConfigSpec.IntValue DISTANCE_ROUNDING_BLOCKS;

    record PassiveLevel(ModConfigSpec.IntValue range, ModConfigSpec.IntValue pingIntervalSeconds) {}

    record ActiveLevel(ModConfigSpec.IntValue durationSeconds, ModConfigSpec.IntValue cooldownSeconds, ModConfigSpec.IntValue arrowRange) {}

    private static final int[] DEFAULT_PASSIVE_RANGES = {24, 32, 48};
    private static final int[] DEFAULT_PING_INTERVALS_SECONDS = {20, 15, 12};
    private static final int[] DEFAULT_HUNT_DURATIONS_SECONDS = {30, 30, 30};
    private static final int[] DEFAULT_HUNT_COOLDOWNS_SECONDS = {90, 75, 60};
    private static final int[] DEFAULT_ARROW_RANGES = {48, 48, 64};
    private static final int MAX_RANGE_BLOCKS = 256;
    private static final int MAX_SECONDS = 3600;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Passive tracking: an occasional ping about the nearest tracked creature.").push("passive");
        for (int i = 0; i < PASSIVE_LEVELS.length; i++) {
            builder.push("level" + (i + 1));
            ModConfigSpec.IntValue range = builder.comment("How far away, in blocks, a creature can be sensed.")
                    .defineInRange("range", DEFAULT_PASSIVE_RANGES[i], 1, MAX_RANGE_BLOCKS);
            ModConfigSpec.IntValue interval = builder.comment("Seconds between pings.")
                    .defineInRange("pingIntervalSeconds", DEFAULT_PING_INTERVALS_SECONDS[i], 1, MAX_SECONDS);
            PASSIVE_LEVELS[i] = new PassiveLevel(range, interval);
            builder.pop();
        }
        HEIGHT_HINT_BLOCKS = builder.comment("Blocks above or below you before a ping (or the hunt arrow) says 'above you' or 'below you'.")
                .defineInRange("heightHintBlocks", 4.0, 0.0, MAX_RANGE_BLOCKS);
        DISTANCE_ROUNDING_BLOCKS = builder.comment("Level 3 pings round the distance to the nearest multiple of this many blocks.")
                .defineInRange("distanceRoundingBlocks", 10, 1, MAX_RANGE_BLOCKS);
        builder.pop();

        builder.comment("Active tracking: the hunt keybind, with a duration and then a cooldown.").push("active");
        for (int i = 0; i < ACTIVE_LEVELS.length; i++) {
            builder.push("level" + (i + 1));
            ModConfigSpec.IntValue duration = builder.comment("How long a hunt lasts, in seconds.")
                    .defineInRange("durationSeconds", DEFAULT_HUNT_DURATIONS_SECONDS[i], 1, MAX_SECONDS);
            ModConfigSpec.IntValue cooldown = builder.comment("Seconds after a hunt ends before the next can start.")
                    .defineInRange("cooldownSeconds", DEFAULT_HUNT_COOLDOWNS_SECONDS[i], 0, MAX_SECONDS);
            ModConfigSpec.IntValue arrowRange = builder.comment("How far away, in blocks, the hunt arrow can find a creature.")
                    .defineInRange("arrowRange", DEFAULT_ARROW_RANGES[i], 1, MAX_RANGE_BLOCKS);
            ACTIVE_LEVELS[i] = new ActiveLevel(duration, cooldown, arrowRange);
            builder.pop();
        }
        GLOW_RANGE = builder.comment("Within this many blocks, level 2 makes the nearest creature glow and level 3 makes all of them glow.")
                .defineInRange("glowRange", 32, 1, MAX_RANGE_BLOCKS);
        HUNT_SCAN_INTERVAL_TICKS = builder.comment("Ticks between scans during a hunt (20 ticks = 1 second). Lower is more responsive, higher is cheaper.")
                .defineInRange("huntScanIntervalTicks", 10, 1, 200);
        HUNT_HOLD_SECONDS = builder.comment("How long the hunt key must be held before a hunt starts, in seconds. 0 starts it on press.")
                .defineInRange("huntHoldSeconds", 2, 0, 10);
        builder.pop();

        SPEC = builder.build();
    }

    private TrackingConfig() {}
}
