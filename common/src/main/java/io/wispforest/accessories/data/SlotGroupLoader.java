package io.wispforest.accessories.data;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.SlotGroup;
import io.wispforest.accessories.impl.SlotGroupImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;

import java.util.*;

public class SlotGroupLoader extends ReplaceableJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final SlotGroupLoader INSTANCE = new SlotGroupLoader();

    private final Map<String, SlotGroup> server = new HashMap<>();
    private final Map<String, SlotGroup> client = new HashMap<>();

    protected SlotGroupLoader() {
        super(GSON, LOGGER, "accessories/group");
    }

    public final Map<String, SlotGroup> getGroups(boolean isClientSide){
        return isClientSide ? this.client : this.server;
    }

    public final SlotGroup getGroup(boolean isClientSide, String group){
        return getGroups(isClientSide).get(group);
    }

    public final Optional<SlotGroup> findGroup(boolean isClientSide, String slot){
        for (var entry : getGroups(isClientSide).values()) {
            if(entry.slots().contains(slot)) return Optional.of(entry);
        }

        return Optional.empty();
    }

    public final SlotGroup getOrDefaultGroup(boolean isClientSide, String slot){
        var groups = getGroups(isClientSide);

        for (var entry : groups.values()) {
            if(entry.slots().contains(slot)) return entry;
        }

        return groups.get("any");
    }

    public final void setGroups(Map<String, SlotGroup> groups){
        this.client.clear();
        this.client.putAll(groups);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.server.clear();

        var slotGroups = new HashMap<String, SlotGroupBuilder>();

        slotGroups.put("any", new SlotGroupBuilder("any").order(120));

        var allSlots = new HashMap<>(SlotTypeLoader.INSTANCE.getSlotTypes(false));

        for (var resourceEntry : data.entrySet()) {
            var location = resourceEntry.getKey();
            var jsonObject = resourceEntry.getValue();

            if(!AccessoriesAccess.getInternal().isValidOnConditions(jsonObject)) continue;

            var pathParts = location.getPath().split("/");

            var group = new SlotGroupBuilder(pathParts[pathParts.length - 1]);

            var slotElements = safeHelper(GsonHelper::getAsJsonArray, jsonObject, "slots", new JsonArray(), location);

            decodeJsonArray(slotElements, "slot", location, JsonElement::getAsString, s -> {
                for (var builderEntry : slotGroups.entrySet()) {
                    if(builderEntry.getValue().slots.contains(s)){
                        LOGGER.error("Unable to assign a give slot [" + s + "] to the group [" + group + "] as it already exists within the group [" + builderEntry.getKey() + "]");
                        return;
                    }
                }

                var slotType = allSlots.remove(s);

                if(slotType == null){
                    LOGGER.warn("SlotType added to a given group without being in the main map for slots!");
                }

                group.addSlot(s);
            });

            group.order(safeHelper(GsonHelper::getAsInt, jsonObject, "order", 100, location));

            slotGroups.put(group.name, group);
        }

        slotGroups.get("any").addSlots(allSlots.keySet());

        slotGroups.forEach((s, builder) -> server.put(s, builder.build()));
    }

    public static class SlotGroupBuilder {
        private final String name;

        private Integer order = null;
        private final Set<String> slots = new HashSet<>();

        public SlotGroupBuilder(String name){
            this.name = name;
        }

        public SlotGroupBuilder order(Integer value){
            this.order = value;

            return this;
        }

        public SlotGroupBuilder addSlot(String value){
            this.slots.add(value);

            return this;
        }

        public SlotGroupBuilder addSlots(Collection<String> values){
            this.slots.addAll(values);

            return this;
        }

        public SlotGroup build(){
            return new SlotGroupImpl(
                    name,
                    Optional.ofNullable(order).orElse(0),
                    slots
            );
        }
    }
}
