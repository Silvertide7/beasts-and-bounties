package net.silvertide.beasts_and_bounties.feature.bestiary.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

public final class EntityPreview {
    private static final float DEPTH = 50f;
    private static final int FULL_BRIGHT = 15728880;

    private EntityPreview() {}

    public static void render(GuiGraphics graphics, float centerX, float centerY, float pixelsPerBlock, LivingEntity entity, MultiBufferSource buffers) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, DEPTH);
        graphics.pose().scale(pixelsPerBlock, pixelsPerBlock, -pixelsPerBlock);
        graphics.pose().translate(0, entity.getBbHeight() / 2f, 0);
        graphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.PI));
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0f, 1f, graphics.pose(), buffers, FULL_BRIGHT));
        graphics.flush();
        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }
}
