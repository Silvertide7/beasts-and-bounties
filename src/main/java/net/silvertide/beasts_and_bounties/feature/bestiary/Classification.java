package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.Optional;

public enum Classification implements StringRepresentable {
    NORMAL("normal", ChatFormatting.BLACK),
    ELITE("elite", ChatFormatting.DARK_AQUA),
    MINOR_BOSS("minor_boss", ChatFormatting.DARK_PURPLE),
    MAJOR_BOSS("major_boss", ChatFormatting.DARK_RED);

    public static final Codec<Classification> CODEC = StringRepresentable.fromEnum(Classification::values);

    private final String serializedName;
    private final ChatFormatting nameColor;

    Classification(String serializedName, ChatFormatting nameColor) {
        this.serializedName = serializedName;
        this.nameColor = nameColor;
    }

    public ChatFormatting nameColor() {
        return nameColor;
    }

    public Optional<ResourceLocation> iconSprite() {
        return this == NORMAL ? Optional.empty() : Optional.of(BeastsAndBountiesMod.id("bestiary/classification_" + serializedName));
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
