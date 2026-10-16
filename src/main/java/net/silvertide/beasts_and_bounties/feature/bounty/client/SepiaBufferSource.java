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
            public VertexConsumer addVertex(float x, float y, float z) {
                buffer.addVertex(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer setColor(int red, int green, int blue, int alpha) {
                buffer.setColor(Math.round(red * RED), Math.round(green * GREEN), Math.round(blue * BLUE), alpha);
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                buffer.setUv(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                buffer.setUv1(u, v);
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                buffer.setUv2(u, v);
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                buffer.setNormal(x, y, z);
                return this;
            }
        };
    }
}
