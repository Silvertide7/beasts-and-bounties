package net.silvertide.beasts_and_bounties.feature.bestiary;

public record Bonuses(
        double damageMultiplier,
        double damageTakenMultiplier,
        double extraLootChance,
        double experienceMultiplier,
        int passiveTracking,
        int activeTracking
) {
    public static final Bonuses NONE = new Bonuses(1, 1, 0, 1, 0, 0);

    static Bonuses fromTotals(double damageBonus, double damageResistance, double extraLootChance, double bonusExperience,
                              int passiveTracking, int activeTracking) {
        return new Bonuses(1 + damageBonus, Math.max(0, 1 - damageResistance), Math.min(1, extraLootChance),
                Math.max(0, 1 + bonusExperience), passiveTracking, activeTracking);
    }

    public boolean hasTracking() {
        return passiveTracking > 0 || activeTracking > 0;
    }
}
