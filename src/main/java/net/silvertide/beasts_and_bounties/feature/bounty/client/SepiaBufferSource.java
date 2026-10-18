package net.silvertide.beasts_and_bounties.feature.bounty.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

final class SepiaBufferSource implements MultiBufferSource {
    private static final float RED = 0.85f;
    private static final float GREEN = 0.72f;
    private static final float BLUE = 0.52f;

    private final MultiBufferSource delegate;

    SepiaBufferSource(MultiBufferSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        VertexConsumer buffer = delegate.getBuffer(renderType);
        return new VertexConsumer() {
            @Override
            public VertexConsumer vertex(double x, double y, double z) {
                buffer.vertex(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer color(int red, int green, int blue, int alpha) {
                buffer.color(Math.round(red * RED), Math.round(green * GREEN), Math.round(blue * BLUE), alpha);
                return this;
            }

            @Override
            public VertexConsumer uv(float u, float v) {
                buffer.uv(u, v);
                return this;
            }

            @Override
            public VertexConsumer overlayCoords(int u, int v) {
                buffer.overlayCoords(u, v);
                return this;
            }

            @Override
            public VertexConsumer uv2(int u, int v) {
                buffer.uv2(u, v);
                return this;
            }

            @Override
            public VertexConsumer normal(float x, float y, float z) {
                buffer.normal(x, y, z);
                return this;
            }

            @Override
            public void endVertex() {
                buffer.endVertex();
            }

            @Override
            public void defaultColor(int red, int green, int blue, int alpha) {
                buffer.defaultColor(Math.round(red * RED), Math.round(green * GREEN), Math.round(blue * BLUE), alpha);
            }

            @Override
            public void unsetDefaultColor() {
                buffer.unsetDefaultColor();
            }
        };
    }
}
