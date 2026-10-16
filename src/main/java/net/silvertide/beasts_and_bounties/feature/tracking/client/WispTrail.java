package net.silvertide.beasts_and_bounties.feature.tracking.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingClientConfig;
import net.silvertide.beasts_and_bounties.feature.tracking.TrackingParticles;
import org.jetbrains.annotations.Nullable;

final class WispTrail {
    private static final double START_AHEAD_OF_EYES = 1.5;
    private static final double EYE_DROP = 0.3;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 NORTH = new Vec3(0, 0, -1);

    @Nullable
    private static Vec3 start;
    private static Vec3 heading = Vec3.ZERO;
    private static Vec3 sideways = Vec3.ZERO;
    private static Vec3 upwards = Vec3.ZERO;
    private static double length;
    private static int ticksTravelled;

    private WispTrail() {}

    static void launchTowards(Vec3 creaturePosition) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Vec3 eyes = player.getEyePosition().subtract(0, EYE_DROP, 0);
        Vec3 toCreature = creaturePosition.subtract(eyes);
        double trailLength = Math.min(TrackingClientConfig.WISP_MAX_LENGTH_BLOCKS.get(),
                toCreature.length() - START_AHEAD_OF_EYES - TrackingClientConfig.WISP_STOP_SHORT_BLOCKS.get());
        if (trailLength <= 0) return;
        heading = toCreature.normalize();
        Vec3 perpendicularSeed = Math.abs(heading.y) > 0.99 ? NORTH : UP;
        sideways = heading.cross(perpendicularSeed).normalize();
        upwards = sideways.cross(heading).normalize();
        start = eyes.add(heading.scale(START_AHEAD_OF_EYES));
        length = trailLength;
        ticksTravelled = 0;
    }

    static void stop() {
        start = null;
    }

    static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (start == null || level == null) return;
        double travelled = ticksTravelled * TrackingClientConfig.WISP_BLOCKS_PER_TICK.get();
        if (travelled > length) {
            stop();
            return;
        }
        double angle = ticksTravelled * TrackingClientConfig.WISP_SWIRL_RADIANS_PER_TICK.get();
        double radius = TrackingClientConfig.WISP_SWIRL_RADIUS.get();
        Vec3 swirl = sideways.scale(Math.cos(angle) * radius).add(upwards.scale(Math.sin(angle) * radius));
        Vec3 at = start.add(heading.scale(travelled)).add(swirl);
        level.addParticle(TrackingParticles.TRACKING_WISP.get(), at.x, at.y, at.z, 0, 0, 0);
        ticksTravelled++;
    }
}
