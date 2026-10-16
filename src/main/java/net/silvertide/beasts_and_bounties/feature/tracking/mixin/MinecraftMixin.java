package net.silvertide.beasts_and_bounties.feature.tracking.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.silvertide.beasts_and_bounties.feature.tracking.client.ClientTracking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void beasts_and_bounties$glowHuntedCreatures(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (ClientTracking.isGlowing(entity)) callback.setReturnValue(true);
    }
}
