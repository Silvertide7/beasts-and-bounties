package net.silvertide.beasts_and_bounties.core;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.silvertide.beasts_and_bounties.BeastsAndBountiesMod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlayerData<T> {
    private record Cached<T>(CompoundTag persisted, Optional<T> value) {}

    private final String key;
    private final Codec<T> codec;
    private final Map<UUID, Cached<T>> cache = new HashMap<>();

    public PlayerData(String name, Codec<T> codec) {
        this.key = BeastsAndBountiesMod.MODID + ":" + name;
        this.codec = codec;
    }

    public Optional<T> get(Player player) {
        CompoundTag persisted = persisted(player);
        Cached<T> cached = cache.get(player.getUUID());
        if (cached == null || cached.persisted != persisted) {
            cached = new Cached<>(persisted, read(persisted));
            cache.put(player.getUUID(), cached);
        }
        return cached.value;
    }

    public T getOrCache(Player player, Supplier<T> fresh) {
        return get(player).orElseGet(() -> {
            T value = fresh.get();
            cache.put(player.getUUID(), new Cached<>(persisted(player), Optional.of(value)));
            return value;
        });
    }

    public void set(Player player, T value) {
        Tag tag = codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, error -> {});
        CompoundTag persisted = persisted(player);
        persisted.put(key, tag);
        cache.put(player.getUUID(), new Cached<>(persisted, Optional.of(value)));
    }

    public void remove(Player player) {
        CompoundTag persisted = persisted(player);
        persisted.remove(key);
        cache.put(player.getUUID(), new Cached<>(persisted, Optional.empty()));
    }

    public void forget(UUID playerId) {
        cache.remove(playerId);
    }

    private Optional<T> read(CompoundTag persisted) {
        if (!persisted.contains(key)) return Optional.empty();
        return codec.parse(NbtOps.INSTANCE, persisted.get(key)).result();
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
