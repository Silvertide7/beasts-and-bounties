package net.silvertide.beasts_and_bounties.feature.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

@Mod.EventBusSubscriber(modid = BeastsAndBountiesMod.MODID)
public final class CombatTrackingEvents {
    private CombatTrackingEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void recordIncomingDamage(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel)) return;
        float dealt = Math.min(event.getAmount(), victim.getHealth() + victim.getAbsorptionAmount());
        if (!Float.isFinite(dealt) || dealt <= 0.0f) return;
        if (victim.getHealth() >= victim.getMaxHealth() && victim.getAbsorptionAmount() <= 0.0f) CombatTracking.clearLog(victim);
        CombatTracking.recordDamage(victim, event.getSource().getEntity(), dealt);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void endEngagementWhenHealedToFull(LivingHealEvent event) {
        LivingEntity healed = event.getEntity();
        if (!(healed.level() instanceof ServerLevel) || healed.getHealth() <= 0.0f) return;
        if (Float.isNaN(event.getAmount()) || healed.getHealth() + event.getAmount() < healed.getMaxHealth()) return;
        if (CombatTracking.hasLog(healed)) CombatTracking.clearLog(healed);
    }

    @SubscribeEvent
    public static void recordAppliedEffect(MobEffectEvent.Added event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel)) return;
        MobEffectInstance applied = event.getEffectInstance();
        boolean beneficial = applied.getEffect().getCategory() == MobEffectCategory.BENEFICIAL;
        if ((target instanceof Player) != beneficial) return;
        if (!improvesOnExistingEffect(event.getOldEffectInstance(), applied)) return;
        CombatTracking.recordEffect(target, event.getEffectSource());
    }

    private static boolean improvesOnExistingEffect(MobEffectInstance existing, MobEffectInstance applied) {
        if (existing == null) return true;
        if (applied.getAmplifier() != existing.getAmplifier()) return applied.getAmplifier() > existing.getAmplifier();
        if (existing.isInfiniteDuration()) return false;
        return applied.isInfiniteDuration() || applied.getDuration() > existing.getDuration();
    }
}
