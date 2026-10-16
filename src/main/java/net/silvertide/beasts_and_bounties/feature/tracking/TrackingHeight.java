package net.silvertide.beasts_and_bounties.feature.tracking;

import java.util.Locale;
import java.util.Optional;

public enum TrackingHeight {
    ABOVE, BELOW;

    public static Optional<TrackingHeight> of(double heightDifference) {
        double threshold = TrackingConfig.HEIGHT_HINT_BLOCKS.get();
        if (heightDifference > threshold) return Optional.of(ABOVE);
        if (heightDifference < -threshold) return Optional.of(BELOW);
        return Optional.empty();
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
