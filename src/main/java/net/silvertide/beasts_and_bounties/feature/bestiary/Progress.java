package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;

public record Progress(int kills, int bounties) {
    public static final Progress NONE = new Progress(0, 0);

    public static final MapCodec<Progress> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("kills", 0).forGetter(Progress::kills),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("bounties", 0).forGetter(Progress::bounties)
    ).apply(instance, Progress::new));
    public static final Codec<Progress> CODEC = MAP_CODEC.codec();

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(kills);
        buf.writeVarInt(bounties);
    }

    public static Progress read(FriendlyByteBuf buf) {
        return new Progress(buf.readVarInt(), buf.readVarInt());
    }

    public boolean meets(Progress requirement) {
        return kills >= requirement.kills && bounties >= requirement.bounties;
    }

    public Progress withKills(int newKills) {
        return new Progress(Math.max(0, newKills), bounties);
    }

    public Progress withBounties(int newBounties) {
        return new Progress(kills, Math.max(0, newBounties));
    }
}
