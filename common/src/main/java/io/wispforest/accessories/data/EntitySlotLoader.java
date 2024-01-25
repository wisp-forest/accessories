package io.wispforest.accessories.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.SlotType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EntitySlotLoader extends ReplaceableJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public static final EntitySlotLoader INSTANCE = new EntitySlotLoader();

    private final Map<EntityType<?>, Map<String, SlotType>> server = new HashMap<>();
    private final Map<EntityType<?>, Map<String, SlotType>> client = new HashMap<>();

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

            if(AccessoriesAccess.isValidOnConditions(jsonObject)) continue;

            var slots = new HashMap<String, SlotType>();

            var slotElements = this.safeHelper(GsonHelper::getAsJsonArray, jsonObject, "slots", new JsonArray(), location);

            this.decodeJsonArray(slotElements, "slot", location, element -> allSlotTypes.get(element.getAsString()), slotType -> {
                slots.put(slotType.name(), slotType);
            });

            //--

            var entities = new ArrayList<EntityType<?>>();

            var entityElements = this.safeHelper(GsonHelper::getAsJsonArray, jsonObject, "slots", new JsonArray(), location);

            this.decodeJsonArray(entityElements, "entity", location, element -> {
                var entityTypeLocation = ResourceLocation.tryParse(element.getAsString());

                return (entityTypeLocation != null)
                        ? BuiltInRegistries.ENTITY_TYPE.get(entityTypeLocation)
                        : null;
            }, entities::add);

            for (EntityType<?> entity : entities) {
                server.computeIfAbsent(entity, entityType -> new HashMap<>()).putAll(slots);
            }
        }
    }
}
