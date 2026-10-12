package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum Tier implements StringRepresentable {
    LOW("low", ChatFormatting.DARK_GREEN),
    MEDIUM("medium", ChatFormatting.GOLD),
    HIGH("high", ChatFormatting.RED),
    EXTREME("extreme", ChatFormatting.DARK_RED);

    public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);

    private final String serializedName;
    private final ChatFormatting color;

    Tier(String serializedName, ChatFormatting color) {
        this.serializedName = serializedName;
        this.color = color;
    }

    public ChatFormatting color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
