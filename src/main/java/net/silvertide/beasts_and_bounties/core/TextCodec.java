package net.silvertide.beasts_and_bounties.core;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

public final class TextCodec {
    public static final Codec<Component> CODEC = ExtraCodecs.JSON.flatXmap(json -> {
        try {
            Component component = Component.Serializer.fromJson(json);
            return component == null ? DataResult.error(() -> "Text is null") : DataResult.success(component);
        } catch (JsonParseException exception) {
            return DataResult.error(() -> "Invalid text: " + exception.getMessage());
        }
    }, component -> DataResult.success(Component.Serializer.toJsonTree(component)));

    private TextCodec() {}
}
