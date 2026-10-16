package net.silvertide.beasts_and_bounties.feature.tracking.mixin;

import net.minecraft.world.entity.Entity;
import net.silvertide.beasts_and_bounties.feature.tracking.client.ClientTracking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void beasts_and_bounties$tintHuntedCreatures(CallbackInfoReturnable<Integer> callback) {
        if (ClientTracking.isGlowing((Entity) (Object) this)) callback.setReturnValue(ClientTracking.GLOW_COLOR);
    }
}
