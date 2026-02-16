package io.wispforest.accessories.data;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.api.ManagedEndecDataLoader;
import io.wispforest.accessories.data.api.SyncedDataHelper;
import io.wispforest.accessories.data.api.SyncedDataHelperManager;
import io.wispforest.accessories.impl.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.impl.slot.SlotGroupImpl;
import io.wispforest.accessories.pond.ReplaceableJsonResourceReloadListener;
import io.wispforest.accessories.utils.CollectionUtils;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class SlotGroupLoader extends ManagedEndecDataLoader<SlotGroup, SlotGroupLoader.RawGroupData> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final SlotGroupLoader INSTANCE = new SlotGroupLoader();

    protected SlotGroupLoader() {
        super(Accessories.of("slot_group_loader"), "accessories/group", SlotGroupImpl.ENDEC, RawGroupData.ENDEC, PackType.SERVER_DATA, Set.of(SlotTypeLoader.INSTANCE.getId()));

        ReplaceableJsonResourceReloadListener.toggleValue(this);

        SyncedDataHelperManager.registerLoader(SyncedDataHelper.of(
            Accessories.of("unique_slot_groups"),
            Endec.STRING.setOf(),
            UniqueSlotHandling::setClientGroups,
            () -> UniqueSlotHandling.getGroups(false),
            this.getId()
        ));

        SyncedDataHelperManager.registerLoader(SyncedDataHelper.of(
            Accessories.of("extra_slot_type_properties"),
            ExtraSlotTypeProperties.ENDEC.mapOf(),
            ExtraSlotTypeProperties::setClientPropertyMap,
            () -> ExtraSlotTypeProperties.getProperties(false),
            this.getId()
        ));
    }

    //--

    public static List<SlotGroup> getGroups(Level level){
        return INSTANCE.getGroups(level.isClientSide(), true);
    }

    public static List<SlotGroup> getGroups(Level level, boolean filterUniqueGroups){
        return INSTANCE.getGroups(level.isClientSide(), filterUniqueGroups);
    }

    public static Map<SlotGroup, List<SlotType>> getValidGroups(LivingEntity living) {
        var entitySpecificSlots = EntitySlotLoader.getEntitySlots(living);

        var groups = SlotGroupLoader.getGroups(living.level(), false);

        return groups.stream()
                .map(slotGroup -> {
                    if(UniqueSlotHandling.isUniqueGroup(slotGroup.name(), living.level().isClientSide())) return null;

                    var slots = slotGroup.slots()
                            .stream()
                            .filter(entitySpecificSlots::containsKey)
                            .map(slot -> SlotTypeLoader.getSlotType(living.level(), slot))
                            .toList();

                    return slots.isEmpty() ? null : Map.entry(slotGroup, slots);
                })
                .filter(Objects::nonNull)
                .collect(CollectionUtils.linkedMapCollector());
    }

    public static Optional<SlotGroup> getGroup(Level level, String group){
        return Optional.ofNullable(INSTANCE.getGroup(level.isClientSide(), group));
    }

    //--

    public final List<SlotGroup> getGroups(boolean isClientSide, boolean filterUniqueGroups){
        var groups = getEntries(isClientSide).values();

        if(filterUniqueGroups) groups = groups.stream().filter(group -> !UniqueSlotHandling.isUniqueGroup(group.name(), isClientSide)).toList();

        return List.copyOf(groups);
    }

    public final SlotGroup getGroup(boolean isClientSide, String group){
        return getEntry(Accessories.parseLocationOrDefault(group), isClientSide);
    }

    public final Optional<SlotGroup> findGroup(boolean isClientSide, String slot){
        for (var entry : getGroups(isClientSide, false)) {
            if(entry.slots().contains(slot)) return Optional.of(entry);
        }

        return Optional.empty();
    }

    public final SlotGroup getOrDefaultGroup(boolean isClientSide, String slot){
        var groups = getEntries(isClientSide);

        for (var entry : groups.values()) {
            if(entry.slots().contains(slot)) return entry;
        }

        return groups.get(Accessories.parseLocationOrDefault(AccessoriesBaseData.ANY_SLOT));
    }

    public record RawGroupData(int order, Set<String> slots, ResourceLocation icon) {
        public static final StructEndec<RawGroupData> ENDEC = StructEndecBuilder.of(
                Endec.INT.fieldOf("order", RawGroupData::order),
                EndecUtils.<Set<String>, String>collectionOf(Endec.STRING, LinkedHashSet::new).fieldOf("slots", RawGroupData::slots),
                MinecraftEndecs.IDENTIFIER.optionalFieldOf("icon", RawGroupData::icon, () -> SlotGroup.UNKNOWN),
                RawGroupData::new
        );
    }

    @Override
    public Map<ResourceLocation, SlotGroup> mapFrom(Map<ResourceLocation, RawGroupData> rawData) {
        var slotGroups = new LinkedHashMap<String, SlotGroupBuilder>();

        slotGroups.put("unsorted", new SlotGroupBuilder("unsorted").order(30));

        var allSlots = new LinkedHashMap<>(SlotTypeLoader.INSTANCE.getEntries(false));

        //--

        rawData.forEach((location, rawGroupData) -> {
            var pathParts = location.getPath().split("/");

            var groupName = pathParts[pathParts.length - 1];
            var namespace = pathParts.length > 1 ? pathParts[0] + ":" : "";

            var isShared = namespace.isBlank();

            if(!isShared) groupName = namespace + ":" + groupName;

            var group = slotGroups.computeIfAbsent(groupName, SlotGroupBuilder::new);

            if(isShared) {
                for (var s : rawGroupData.slots()) {
                    var slotType = allSlots.remove(Accessories.parseLocationOrDefault(s));

                    boolean isValid = true;

                    if (slotType == null) {
                        LOGGER.warn("Slot '{}' for the given group '{}' was not found to be loaded, it will be ignored!", s, groupName);

                        isValid = false;
                    } else {
                        for (var builderEntry : slotGroups.entrySet()) {
                            if (builderEntry.getValue().slots.contains(slotType)) {
                                LOGGER.error("Unable to assign a give slot '{}' to the group '{}' as it already exists within the group '{}'", s, group, builderEntry.getKey());
                                isValid = false;
                            }
                        }
                    }

                    if (isValid) group.addSlot(slotType);
                }

                group.order(rawGroupData.order());
            }

            group.icon(rawGroupData.icon());
        });

        //--

        var remainSlots = new HashSet<SlotType>();

        for (var value : allSlots.values()) {
            var slotName = value.name();

            if(!UniqueSlotHandling.isUniqueSlot(slotName)) {
                remainSlots.add(value);

                continue;
            }

            var group = slotName.split(":")[0];

            slotGroups.computeIfAbsent(group, SlotGroupBuilder::new)
                    .order(5)
                    .addSlot(value);

            UniqueSlotHandling.addGroup(group);
        }

        slotGroups.get("unsorted").addSlots(remainSlots);

        return slotGroups.entrySet().stream()
            .map(entry -> Map.entry(Accessories.parseLocationOrDefault(entry.getKey()), entry.getValue().build()))
            .sorted(Map.Entry.<ResourceLocation, SlotGroup>comparingByValue().reversed())
            .collect(CollectionUtils.linkedMapCollector());
    }

    public static class SlotGroupBuilder {
        private final String name;

        private Integer order = null;
        private final Set<SlotType> slots = new HashSet<>();

        private ResourceLocation iconLocation = SlotGroup.UNKNOWN;

        public SlotGroupBuilder(String name){
            this.name = name;
        }

        public SlotGroupBuilder order(Integer value){
            this.order = value;

            return this;
        }

        public SlotGroupBuilder addSlot(SlotType value){
            this.slots.add(value);

            return this;
        }

        public SlotGroupBuilder addSlots(Collection<SlotType> values){
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
                    slots.stream().sorted(Comparator.<SlotType>naturalOrder().reversed()).map(SlotType::name).collect(Collectors.toCollection(LinkedHashSet::new)),
                    iconLocation
            );
        }
    }
}
