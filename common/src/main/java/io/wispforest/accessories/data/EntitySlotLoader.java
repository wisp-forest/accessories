package io.wispforest.accessories.data;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.api.EndecDataLoader;
import io.wispforest.accessories.data.api.SyncedDataHelper;
import io.wispforest.accessories.impl.core.AccessoriesHolderImpl;
import io.wispforest.accessories.impl.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.impl.slot.StrictMode;
import io.wispforest.accessories.pond.ReplaceableJsonResourceReloadListener;
import io.wispforest.accessories.utils.CollectionUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resource Reload in which handles the loading of {@link SlotType}'s bindings
 * to the targeted {@link EntityType} though a {@link TagKey} or {@link ResourceLocation}
 */
public class EntitySlotLoader extends EndecDataLoader<EntitySlotLoader.RawEnityBinding> implements SyncedDataHelper<SequencedMap<EntityType<?>, List<String>>> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final EntitySlotLoader INSTANCE = new EntitySlotLoader();

    private Map<TagKey<EntityType<?>>, Map<String, SlotType>> tagToBoundSlots = new HashMap<>();
    private Map<EntityType<?>, Map<String, SlotType>> entityToBoundSlots = new HashMap<>();

    private SequencedMap<EntityType<?>, SequencedMap<String, SlotType>> server = new LinkedHashMap<>();
    private SequencedMap<EntityType<?>, SequencedMap<String, SlotType>> client = new LinkedHashMap<>();

    protected EntitySlotLoader() {
        super(Accessories.of("entity_slot_loader"), "accessories/entity", RawEnityBinding.ENDEC, PackType.SERVER_DATA, Set.of(SlotTypeLoader.INSTANCE.getId()));

        ReplaceableJsonResourceReloadListener.toggleValue(this);
    }

    //--

    /**
     * @return The valid {@link SlotType}'s for given {@link LivingEntity} based on its {@link EntityType}
     */
    public static Map<String, SlotType> getEntitySlots(LivingEntity livingEntity){
        return getEntitySlots(livingEntity.level(), livingEntity.getType());
    }

    /**
     * @return The valid {@link SlotType}'s for given {@link EntityType}
     */
    public static Map<String, SlotType> getEntitySlots(Level level, EntityType<?> entityType){
        var map = EntitySlotLoader.INSTANCE.getSlotTypes(level.isClientSide(), entityType);

        return map != null ? map : Map.of();
    }

    //--

    @Nullable
    public final Map<String, SlotType> getSlotTypes(boolean isClientSide, EntityType<?> entityType){
        return this.getEntitySlotData(isClientSide).get(entityType);
    }

    @ApiStatus.Internal
    public final Map<EntityType<?>, Map<String, SlotType>> getEntitySlotData(boolean isClientSide){
        return (Map) (isClientSide ? this.client : this.server);
    }

    //--

    public record RawEnityBinding(Set<String> entityTargets, Set<String> slotTypes) {
        public static final StructEndec<RawEnityBinding> ENDEC = StructEndecBuilder.of(
                Endec.STRING.setOf().fieldOf("entities", RawEnityBinding::entityTargets),
                Endec.STRING.setOf().fieldOf("slots", RawEnityBinding::slotTypes),
                RawEnityBinding::new
        );
    }

    @Override
    public Endec<SequencedMap<EntityType<?>, List<String>>> syncDataEndec() {
        return Endec.map(LinkedHashMap::new,
            type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), strType -> BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.parse(strType)),
            Endec.STRING.listOf());
    }

    @Override
    public void onReceivedData(SequencedMap<EntityType<?>, List<String>> data) {
        SequencedMap<EntityType<?>, SequencedMap<String, SlotType>> entitySlotTypes = new LinkedHashMap<>();

        for (var entry : data.entrySet()) {
            var map = entry.getValue().stream()
                .map(string -> SlotTypeLoader.INSTANCE.getSlotType(true, string))
                .filter(Objects::nonNull)
                .collect(CollectionUtils.linkedMapKeyCollector(SlotType::name));

            entitySlotTypes.put(entry.getKey(), map);
        }

        this.client = Collections.unmodifiableSequencedMap(entitySlotTypes);

        AccessoriesHolderImpl.clearValidationCache(true);
    }

    @Override
    public SequencedMap<EntityType<?>, List<String>> getServerData() {
        var entitySlots = new LinkedHashMap<EntityType<?>, List<String>>();

        for (var entry : server.entrySet()) {
            entitySlots.put(entry.getKey(), List.copyOf(entry.getValue().keySet()));
        }

        return entitySlots;
    }

    public void buildEntryMap() {
        var tempMap = new LinkedHashMap<EntityType<?>, SequencedMap<String, SlotType>>();

        this.tagToBoundSlots.forEach((entityTag, slots) -> {
            var entityTypes = BuiltInRegistries.ENTITY_TYPE.get(entityTag)
                    .map(holders -> holders.stream().map(Holder::value).collect(Collectors.toSet()))
                    .orElseGet(() -> {
                        LOGGER.warn("[EntitySlotLoader]: Unable to locate the given EntityType Tag used within a slot entry: [Location: {}]", entityTag.location());
                        return Set.of();
                    });

            entityTypes.forEach(entityType -> {
                tempMap.computeIfAbsent(entityType, entityType1 -> new LinkedHashMap<>())
                        .putAll(slots);
            });
        });

        this.entityToBoundSlots.forEach((entityType, slots) -> {
            tempMap.computeIfAbsent(entityType, entityType1 -> new LinkedHashMap<>())
                    .putAll(slots);
        });

        var finishMap = new LinkedHashMap<EntityType<?>, SequencedMap<String, SlotType>>();

        tempMap.forEach((entityType, slotsBuilder) -> finishMap.put(entityType, Collections.unmodifiableSequencedMap(slotsBuilder)));

        this.server = finishMap;

        AccessoriesHolderImpl.clearValidationCache(false);

        this.tagToBoundSlots.clear();
        this.entityToBoundSlots.clear();
    }

    //--

    @Override
    protected void apply(Map<ResourceLocation, RawEnityBinding> rawData, ResourceManager resourceManager, ProfilerFiller profiler) {
        var allSlotTypes = SlotTypeLoader.INSTANCE.getEntries(false);

        this.tagToBoundSlots.clear();
        this.entityToBoundSlots.clear();

        for (var resourceEntry : rawData.entrySet()) {
            var location = resourceEntry.getKey();
            var rawEnityBinding = resourceEntry.getValue();

            var slots = new LinkedHashMap<String, SlotType>();

            rawEnityBinding.slotTypes().stream().map(slotName -> {
                return Pair.of(slotName, allSlotTypes.get(Accessories.parseLocationOrDefault(slotName)));
            }).forEach(slotInfo -> {
                var slotType = slotInfo.right();

                if(slotType != null) {
                    if(!ExtraSlotTypeProperties.getProperty(slotInfo.left(), false).strictMode().equals(StrictMode.FULL)) {
                        slots.put(slotType.name(), slotType);
                    } else {
                        LOGGER.warn("Unable to add the given slot [{}] to the given group due to it being in strict mode! [Location: {}]", slotInfo.left(), location);
                    }
                } else if (slotType == null) {
                    LOGGER.warn("Unable to locate a given slot [{}] to add to a given entity('s) as it was not registered: [Location: {}]", slotInfo.first(), location);
                }
            });

            //--

            rawEnityBinding.entityTargets().forEach(string -> {
                if(string.contains("#")){
                    var entityTypeTagLocation = ResourceLocation.tryParse(string.replace("#", ""));

                    var entityTypeTag = TagKey.create(Registries.ENTITY_TYPE, entityTypeTagLocation);

                    tagToBoundSlots.computeIfAbsent(entityTypeTag, entityTag -> new HashMap<>())
                            .putAll(slots);
                } else {
                    Optional.ofNullable(ResourceLocation.tryParse(string))
                            .flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional)
                            .ifPresentOrElse(entityType -> {
                                entityToBoundSlots.computeIfAbsent(entityType, entityType1 -> new HashMap<>())
                                        .putAll(slots);
                            }, () -> {
                                LOGGER.warn("[EntitySlotLoader]: Unable to locate the given EntityType [{}] within the registries for a slot entry: [Location: {}]", string, location);
                            });
                }
            });
        }

        for (var entry : UniqueSlotHandling.getSlotToEntities().entrySet()) {
            var slotType = SlotTypeLoader.INSTANCE.getEntries(false).get(Accessories.parseLocationOrDefault(entry.getKey()));

            for (var entityType : entry.getValue()) {
                entityToBoundSlots.computeIfAbsent(entityType, entityType1 -> new LinkedHashMap<>())
                        .put(slotType.name(), slotType);
            }
        }
    }
}
