package io.wispforest.accessories.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.SlotType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Resource Reload in which handles the loading of {@link SlotType}'s bindings
 * to the targeted {@link EntityType} though a {@link TagKey} or {@link ResourceLocation}
 */
public class EntitySlotLoader extends ReplaceableJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public static final EntitySlotLoader INSTANCE = new EntitySlotLoader();

    private final Map<EntityType<?>, Map<String, SlotType>> server = new HashMap<>();
    private final Map<EntityType<?>, Map<String, SlotType>> client = new HashMap<>();

    public List<Consumer<Map<EntityType<?>, Map<String, SlotType>>>> externalEventHooks = new ArrayList<>();

    protected EntitySlotLoader() {
        super(GSON, LOGGER, "accessories/entities");
    }

    @Nullable
    public final Map<String, SlotType> getSlotTypes(boolean isClientSide, EntityType<?> entityType){
        return getEntitySlotData(isClientSide).get(entityType);
    }

    @ApiStatus.Internal
    public final Map<EntityType<?>, Map<String, SlotType>> getEntitySlotData(boolean isClientSide){
        return isClientSide ? client : server;
    }

    @ApiStatus.Internal
    public final void setEntitySlotData(Map<EntityType<?>, Map<String, SlotType>> data){
        this.client.clear();
        this.client.putAll(data);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        server.clear();

        var allSlotTypes = SlotTypeLoader.INSTANCE.getSlotTypes(false);

        for (var resourceEntry : data.entrySet()) {
            var location = resourceEntry.getKey();
            var jsonObject = resourceEntry.getValue();

            if(!AccessoriesAccess.getInternal().isValidOnConditions(jsonObject)) continue;

            var slots = new HashMap<String, SlotType>();

            var slotElements = this.safeHelper(GsonHelper::getAsJsonArray, jsonObject, "slots", new JsonArray(), location);

            this.decodeJsonArray(slotElements, "slot", location, element -> allSlotTypes.get(element.getAsString()), slotType -> {
                slots.put(slotType.name(), slotType);
            });

            //--

            var entities = new ArrayList<EntityType<?>>();

            var entityElements = this.safeHelper(GsonHelper::getAsJsonArray, jsonObject, "entities", new JsonArray(), location);

            this.decodeJsonArray(entityElements, "entity", location, element -> {
                var string = element.getAsString();

                if(string.contains("#")){
                    var entityTypeTagLocation = ResourceLocation.tryParse(string.replace("#", ""));

                    var entityTypeTag = TagKey.create(Registries.ENTITY_TYPE, entityTypeTagLocation);

                    return AccessoriesAccess.getHolder(entityTypeTag)
                            .map(holders -> {
                                return holders.stream()
                                        .map(Holder::value)
                                        .collect(Collectors.toSet());
                            }).orElseGet(() -> {
                                LOGGER.warn("[EntitySlotLoader]: Unable to locate the given EntityType Tag used within a slot entry: [Location: " + string + "]");
                                return Set.of();
                            });
                } else {
                    return Optional.ofNullable(ResourceLocation.tryParse(string))
                            .map(location1 -> {
                                return BuiltInRegistries.ENTITY_TYPE.getOptional(location1)
                                        .map(Set::of)
                                        .orElse(Set.of());
                            })
                            .orElseGet(() -> {
                                LOGGER.warn("[EntitySlotLoader]: Unable to locate the given EntityType within the registries for a slot entrie: [Location: " + string + "]");

                                return Set.of();
                            });
                }
            }, entities::addAll);

            for (EntityType<?> entity : entities) {
                server.computeIfAbsent(entity, entityType -> new HashMap<>()).putAll(slots);
            }
        }

        for (var eventHook : externalEventHooks) eventHook.accept(server);
    }
}
