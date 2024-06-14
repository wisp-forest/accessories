package io.wispforest.accessories.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.impl.SlotGroupImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.*;

public class SlotGroupLoader extends ReplaceableJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final SlotGroupLoader INSTANCE = new SlotGroupLoader();

    private Map<String, SlotGroup> server = new HashMap<>();
    private Map<String, SlotGroup> client = new HashMap<>();

    protected SlotGroupLoader() {
        super(GSON, LOGGER, "accessories/group");
    }

    //--

    public static List<SlotGroup> getGroups(Level level){
        return INSTANCE.getGroups(level.isClientSide(), true);
    }

    public static List<SlotGroup> getGroups(Level level, boolean filterUniqueGroups){
        return INSTANCE.getGroups(level.isClientSide(), filterUniqueGroups);
    }

    public static Optional<SlotGroup> getGroup(Level level, String group){
        return Optional.ofNullable(INSTANCE.getGroup(level.isClientSide(), group));
    }

    //--

    @ApiStatus.Internal
    public final Map<String, SlotGroup> getGroupMap(boolean isClientSide) {
        return (isClientSide ? this.client : this.server);
    }

    public final List<SlotGroup> getGroups(boolean isClientSide, boolean filterUniqueGroups){
        var groups = getGroupMap(isClientSide).values();

        if(filterUniqueGroups) groups = groups.stream().filter(group -> !UniqueSlotHandling.isUniqueGroup(group.name())).toList();

        return List.copyOf(groups);
    }

    public final SlotGroup getGroup(boolean isClientSide, String group){
        return getGroupMap(isClientSide).get(group);
    }

    public final Optional<SlotGroup> findGroup(boolean isClientSide, String slot){
        for (var entry : getGroups(isClientSide, false)) {
            if(entry.slots().contains(slot)) return Optional.of(entry);
        }

        return Optional.empty();
    }

    public final SlotGroup getOrDefaultGroup(boolean isClientSide, String slot){
        var groups = getGroupMap(isClientSide);

        for (var entry : groups.values()) {
            if(entry.slots().contains(slot)) return entry;
        }

        return groups.get("any");
    }

    public final void setGroups(Map<String, SlotGroup> groups){
        this.client = ImmutableMap.copyOf(groups);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        var slotGroups = new HashMap<String, SlotGroupBuilder>();

        slotGroups.put("unsorted", new SlotGroupBuilder("unsorted").order(30));

        var allSlots = new HashMap<>(SlotTypeLoader.INSTANCE.getSlotTypes(false));

        for (var resourceEntry : data.entrySet()) {
            var location = resourceEntry.getKey();
            var jsonObject = resourceEntry.getValue();

            if(!AccessoriesInternals.isValidOnConditions(jsonObject)) continue;

            boolean isShared = location.getNamespace().contains(Accessories.MODID);

            var pathParts = location.getPath().split("/");

            String name = pathParts[pathParts.length - 1];

            if(!isShared) name = location.getNamespace() + ":" + name;

            var group = new SlotGroupBuilder(name);

            var slotElements = safeHelper(GsonHelper::getAsJsonArray, jsonObject, "slots", new JsonArray(), location);

            decodeJsonArray(slotElements, "slot", location, JsonElement::getAsString, s -> {
                for (var builderEntry : slotGroups.entrySet()) {
                    if (builderEntry.getValue().slots.contains(s)) {
                        LOGGER.error("Unable to assign a give slot [" + s + "] to the group [" + group + "] as it already exists within the group [" + builderEntry.getKey() + "]");
                        return;
                    }
                }

                var slotType = allSlots.remove(s);

                if (slotType == null) {
                    LOGGER.warn("SlotType added to a given group without being in the main map for slots!");
                }

                group.addSlot(s);
            });

            if(isShared) group.order(safeHelper(GsonHelper::getAsInt, jsonObject, "order", 100, location));

            var icon = safeHelper(GsonHelper::getAsString, jsonObject, "icon", location);

            if(icon != null){
                var iconLocation = ResourceLocation.tryParse(icon);

                if(iconLocation != null){
                    group.icon(iconLocation);
                } else {
                    LOGGER.warn("A given SlotGroup was found to have a invalid Icon Location. [Location: {}]", location);
                }
            }

            slotGroups.put(group.name, group);
        }

        var remainSlots = new HashSet<String>();

        for (var value : allSlots.values()) {
            var slotName = value.name();

            if(!UniqueSlotHandling.isUniqueSlot(slotName)) {
                remainSlots.add(slotName);

                continue;
            }

            var group = slotName.split(":")[0];

            slotGroups.computeIfAbsent(group, SlotGroupBuilder::new)
                    .order(5)
                    .addSlot(slotName);

            UniqueSlotHandling.addGroup(group);
        }

        slotGroups.get("unsorted").addSlots(remainSlots);

        var tempMap = ImmutableMap.<String, SlotGroup>builder();

        slotGroups.forEach((s, builder) -> tempMap.put(s, builder.build()));

        this.server = tempMap.build();
    }

    public static class SlotGroupBuilder {
        private final String name;

        private Integer order = null;
        private final Set<String> slots = new HashSet<>();

        private ResourceLocation iconLocation = SlotGroup.UNKNOWN;

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

        public SlotGroupBuilder icon(ResourceLocation location) {
            this.iconLocation = location;

            return this;
        }

        public SlotGroup build(){
            return new SlotGroupImpl(
                    name,
                    Optional.ofNullable(order).orElse(0),
                    slots,
                    iconLocation
            );
        }
    }
}
