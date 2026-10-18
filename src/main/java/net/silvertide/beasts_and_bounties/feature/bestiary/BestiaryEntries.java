package net.silvertide.beasts_and_bounties.feature.bestiary;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.silvertide.beasts_and_bounties.core.StrictCodecs;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BestiaryEntries extends SimplePreparableReloadListener<Map<ResourceLocation, List<BestiaryEntries.PackFile>>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter FILES = FileToIdConverter.json("bestiary");

    private static Map<ResourceLocation, BestiaryEntry> entriesById = Map.of();
    private static Map<EntityType<?>, BestiaryEntry> entriesByType = Map.of();

    public record PackFile(String packId, JsonElement json) {}

    private record EntryFile(boolean replace, Optional<Classification> classification, List<Milestone> milestones) {
        static final Codec<EntryFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("replace", false).forGetter(EntryFile::replace),
                StrictCodecs.optionalField("classification", Classification.CODEC).forGetter(EntryFile::classification),
                StrictCodecs.optionalField("milestones", Milestone.CODEC.listOf(), List.of()).forGetter(EntryFile::milestones)
        ).apply(instance, EntryFile::new));
    }

    private final RegistryAccess registries;

    BestiaryEntries(RegistryAccess registries) {
        this.registries = registries;
    }

    public static Map<ResourceLocation, BestiaryEntry> all() {
        return entriesById;
    }

    @Nullable
    public static BestiaryEntry get(EntityType<?> entityType) {
        return entriesByType.get(entityType);
    }

    @Nullable
    public static BestiaryEntry get(ResourceLocation entityId) {
        return entriesById.get(entityId);
    }

    @Override
    protected Map<ResourceLocation, List<PackFile>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, List<PackFile>> filesByEntity = new HashMap<>();
        FILES.listMatchingResourceStacks(resourceManager).forEach((file, stack) -> {
            List<PackFile> parsed = new ArrayList<>();
            for (Resource resource : stack) {
                try (Reader reader = resource.openAsReader()) {
                    parsed.add(new PackFile(resource.sourcePackId(), GsonHelper.parse(reader)));
                } catch (IOException | RuntimeException exception) {
                    LOGGER.warn("Skipping bestiary file {} from pack {}: {}", file, resource.sourcePackId(), exception.getMessage());
                }
            }
            filesByEntity.put(FILES.fileToId(file), parsed);
        });
        return filesByEntity;
    }

    @Override
    protected void apply(Map<ResourceLocation, List<PackFile>> filesByEntity, ResourceManager resourceManager, ProfilerFiller profiler) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Map<ResourceLocation, BestiaryEntry> loadedById = new HashMap<>();
        Map<EntityType<?>, BestiaryEntry> loadedByType = new HashMap<>();
        filesByEntity.forEach((entityId, packFiles) -> {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.containsKey(entityId) ? ForgeRegistries.ENTITY_TYPES.getValue(entityId) : null;
            if (entityType == null) {
                LOGGER.warn("Skipping bestiary entry {}: no such entity", entityId);
                return;
            }
            BestiaryEntry entry = mergeInPackOrder(entityId, packFiles, ops);
            if (entry == null) return;
            loadedById.put(entityId, entry);
            loadedByType.put(entityType, entry);
        });
        entriesById = Map.copyOf(loadedById);
        entriesByType = Map.copyOf(loadedByType);
        LOGGER.info("Loaded {} bestiary entries", entriesById.size());
    }

    @Nullable
    public static BestiaryEntry mergeInPackOrder(ResourceLocation entityId, List<PackFile> packFiles, RegistryOps<JsonElement> ops) {
        Classification classification = Classification.NORMAL;
        List<Milestone> milestones = new ArrayList<>();
        for (PackFile packFile : packFiles) {
            DataResult<EntryFile> result = EntryFile.CODEC.parse(ops, packFile.json());
            if (result.error().isPresent()) {
                LOGGER.warn("Skipping bestiary entry {} from pack {}: {}", entityId, packFile.packId(), result.error().get().message());
                continue;
            }
            Optional<EntryFile> parsed = result.result();
            if (parsed.isEmpty()) continue;
            if (parsed.get().replace()) {
                classification = Classification.NORMAL;
                milestones.clear();
            }
            classification = parsed.get().classification().orElse(classification);
            milestones.addAll(parsed.get().milestones());
        }
        if (milestones.isEmpty()) {
            LOGGER.warn("Skipping bestiary entry {}: no milestones", entityId);
            return null;
        }
        return new BestiaryEntry(classification, milestones);
    }
}
