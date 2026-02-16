package io.wispforest.accessories.data;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.events.DropRule;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.api.slot.validator.SlotValidatorRegistry;
import io.wispforest.accessories.data.api.ManagedEndecDataLoader;
import io.wispforest.accessories.impl.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.impl.slot.SlotTypeImpl;
import io.wispforest.accessories.impl.slot.StrictMode;
import io.wispforest.accessories.pond.ReplaceableJsonResourceReloadListener;
import io.wispforest.accessories.utils.EndecUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class SlotTypeLoader extends ManagedEndecDataLoader<SlotType, SlotTypeLoader.RawSlotData> {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final SlotTypeLoader INSTANCE = new SlotTypeLoader();

    protected SlotTypeLoader() {
        super(Accessories.of("slot_loader"), "accessories/slot", SlotTypeImpl.ENDEC, RawSlotData.ENDEC, PackType.SERVER_DATA);

        ReplaceableJsonResourceReloadListener.toggleValue(this);
    }

    private final Map<EntityType<?>, Collection<SlotType>> slotUsedByRegistryItemCache_server = new HashMap<>();
    private final Map<EntityType<?>, Collection<SlotType>> slotUsedByRegistryItemCache_client = new HashMap<>();

    //--

    /**
     * Attempt to get the given SlotType based on the provided slotName
     */
    @Nullable
    public static SlotType getSlotType(LivingEntity entity, String slotName){
        return getSlotType(entity.level(), slotName);
    }

    /**
     * Attempt to get the given SlotType based on the provided slotName
     */
    @Nullable
    public static SlotType getSlotType(Level level, String slotName){
        return INSTANCE.getSlotType(level.isClientSide(), slotName);
    }

    //--

    @Nullable
    public SlotType getSlotType(boolean isClientSide, String slotName){
        return getEntry(Accessories.parseLocationOrDefault(slotName), isClientSide);
    }

    private static Map<EntityType<?>, Collection<SlotType>> getUsedSlots(boolean isClientSide) {
        return isClientSide ? INSTANCE.slotUsedByRegistryItemCache_client : INSTANCE.slotUsedByRegistryItemCache_server;
    }

    public static Collection<SlotType> getUsedSlotsByRegistryItem(LivingEntity living){
        var map = getUsedSlots(living.level().isClientSide());

        if (map.containsKey(living.getType())) return map.get(living.getType());

        var validSlotTypes = new LinkedHashSet<SlotType>();

        BuiltInRegistries.ITEM.forEach(item -> {
            var stack = item.getDefaultInstance();

            var validSlots = SlotValidatorRegistry.getStackSlotTypes(living, stack);

            validSlotTypes.addAll(validSlots);
        });

        map.put(living.getType(), validSlotTypes);

        return validSlotTypes;
    }

    @Override
    protected void onSync() {
        this.slotUsedByRegistryItemCache_client.clear();

        UniqueSlotHandling.buildClientSlotReferences();
    }

    @Override
    public Map<ResourceLocation, SlotType> mapFrom(Map<ResourceLocation, RawSlotData> rawData) {
        var uniqueSlots = new LinkedHashMap<String, SlotBuilder>();

        try {
            UniqueSlotHandling.gatherUniqueSlots((location, integer, slotPredicates) -> {
                var name = location.toString();

                if(uniqueSlots.containsKey(name)) {
                    throw new IllegalStateException("Unable to register the given unique slot as a existing slot has been registered before! [Name: " + name + "]");
                }

                var builder = new SlotBuilder(name);

                builder.amount(integer);

                uniqueSlots.put(name, builder);

                slotPredicates.forEach(builder::validator);

                return () -> name;
            });
        } catch (Exception e) {
            LOGGER.error("[SlotTypeLoader]: Error occurred when trying to gather unique slots though code!", e);
        }

        var builders = new LinkedHashMap<>(uniqueSlots);

        for (var resourceEntry : rawData.entrySet()) {
            var location = resourceEntry.getKey();
            var rawSlotData = resourceEntry.getValue();

            var pathParts = location.getPath().split("/");

            String slotName = pathParts[pathParts.length - 1];
            String namespace = pathParts.length > 1 ? pathParts[0] + ":" : "";

            var slotBuilder = builders.computeIfAbsent(namespace + slotName, SlotBuilder::new);

            slotBuilder.icon(rawSlotData.icon());

            slotBuilder.order(rawSlotData.order());

            if(ExtraSlotTypeProperties.getProperty(slotBuilder.name, false).allowResizing()){
                var amount = rawSlotData.amount;

                if(amount != null) {
                    var operation = rawSlotData.operationType;

                    boolean operationOccured = true;

                    if(operation != null) {
                        switch (operation) {
                            case SET -> slotBuilder.amount(amount);
                            case ADD -> slotBuilder.addAmount(amount);
                            case SUB -> slotBuilder.subtractAmount(amount);
                            case null, default -> {
                                operationOccured = false;
                            }
                        }
                    }

                    if(!operationOccured) {
                        LOGGER.error("Unable to understand the passed operation for the given slot type file! [Location: {}, Operation: {}]", location, operation);
                    }
                }
            }

            if(ExtraSlotTypeProperties.getProperty(slotBuilder.name, false).strictMode().equals(StrictMode.NONE) && rawSlotData.validators() != null) {
                for (var validator : rawSlotData.validators()) {
                    slotBuilder.validator(validator);
                }
            }

            slotBuilder.dropRule(rawSlotData.dropRule());

            builders.put(slotBuilder.name, slotBuilder);
        }

        var tempMap = new HashMap<ResourceLocation, SlotType>();

        for (var modifier : Accessories.config().modifiers()) {
            var builder = builders.getOrDefault(modifier.slotType, null);

            if(builder == null) continue;

            builder.addAmount(modifier.amount);
        }

        builders.forEach((s, slotBuilder) -> {
            if(s.equals(AccessoriesBaseData.ANY_SLOT)) return;

            tempMap.put(Accessories.parseLocationOrDefault(s), slotBuilder.create());
        });

        this.slotUsedByRegistryItemCache_server.clear();

        return tempMap;
    }

    public record RawSlotData(@Nullable ResourceLocation icon,
                              @Nullable Integer order,
                              @Nullable Integer amount,
                              @Nullable OperationType operationType,
                              @Nullable Set<ResourceLocation> validators,
                              @Nullable DropRule dropRule) {
        public static final StructEndec<RawSlotData> ENDEC = StructEndecBuilder.of(
                MinecraftEndecs.IDENTIFIER.optionalFieldOf("icon", RawSlotData::icon, () -> null),
                Endec.INT.optionalFieldOf("order", RawSlotData::order, () -> null),
                Endec.INT.optionalFieldOf("amount", RawSlotData::amount, () -> null),
                EndecUtils.forEnum(OperationType.class).optionalFieldOf("operation", RawSlotData::operationType, () -> null),
                MinecraftEndecs.IDENTIFIER.setOf().optionalFieldOf("validators", RawSlotData::validators, () -> null),
                EndecUtils.forEnum(DropRule.class).optionalFieldOf("dropRule", RawSlotData::dropRule, () -> null),
                RawSlotData::new
        );
    }

    public static class SlotBuilder {
        private final String name;
        private ResourceLocation icon = null;
        private Integer order = null;

        public Integer baseAmount = null;
        private Integer offsetAmount = 0;

        private final Set<ResourceLocation> validators = new HashSet<>();
        private DropRule dropRule = null;

        private Optional<String> alternativeTranslation = Optional.empty();

        public SlotBuilder(String name){
            this.name = name;
        }

        public SlotBuilder alternativeTranslation(String value){
            this.alternativeTranslation = Optional.of(value);
            return this;
        }

        public SlotBuilder icon(ResourceLocation value){
            if (value != null) this.icon = value;
            return this;
        }

        public SlotBuilder order(Integer value){
            if (value != null) this.order = value;
            return this;
        }

        public SlotBuilder amount(int value){
            this.baseAmount = value;
            return this;
        }

        public SlotBuilder addAmount(int value){
            this.offsetAmount += value;
            return this;
        }

        public SlotBuilder subtractAmount(int value){
            this.offsetAmount -= value;
            return this;
        }

        public SlotBuilder validator(ResourceLocation validator){
            this.validators.add(validator);
            return this;
        }

        public SlotBuilder dropRule(DropRule value){
            if (value != null) this.dropRule = value;
            return this;
        }

        public SlotType create(){
            if(this.validators.isEmpty()) {
                this.validators.add(Accessories.of("tag"));
                this.validators.add(Accessories.of("component"));
            }

            var defaultedBaseAmount = Optional.ofNullable(this.baseAmount).map(i -> Math.max(i, 0)).orElse(1);

            defaultedBaseAmount = this.offsetAmount + defaultedBaseAmount;

            return new SlotTypeImpl(
                    this.name,
                    this.alternativeTranslation,
                    Optional.ofNullable(this.icon).orElse(SlotType.EMPTY_SLOT_ICON),
                    Optional.ofNullable(this.order).orElse(1000),
                    defaultedBaseAmount,
                    this.validators,
                    Optional.ofNullable(this.dropRule).orElse(DropRule.DEFAULT)
            );
        }
    }
}
