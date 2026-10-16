package net.silvertide.beasts_and_bounties.feature.bounty;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class BountyDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Map<ResourceLocation, BountyDefinition> definitions = Map.of();

    private final HolderLookup.Provider registries;

    BountyDefinitions(HolderLookup.Provider registries) {
        super(new Gson(), "bounty");
        this.registries = registries;
    }

    public static Map<ResourceLocation, BountyDefinition> all() {
        return definitions;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Map<ResourceLocation, BountyDefinition> loaded = new HashMap<>();
        files.forEach((entityId, json) -> {
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
                LOGGER.warn("Skipping bounty {}: no such entity", entityId);
                return;
            }
            BountyDefinition.CODEC.parse(ops, json)
                    .ifError(error -> LOGGER.warn("Skipping bounty {}: {}", entityId, error.message()))
                    .ifSuccess(definition -> loaded.put(entityId, definition));
        });
        definitions = Map.copyOf(loaded);
        LOGGER.info("Loaded {} bounties", definitions.size());
    }
}
