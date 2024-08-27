package io.wispforest.accessories.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.*;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.impl.SlotTypeImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class SlotTypeLoader extends ReplaceableJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public static final SlotTypeLoader INSTANCE = new SlotTypeLoader();

    protected SlotTypeLoader() {
        super(GSON, LOGGER, "accessories/slot");
    }

    private Map<String, SlotType> server = new HashMap<>();
    private Map<String, SlotType> client = new HashMap<>();

    //--

    /**
     * Attempt to get the given SlotType based on the provided slotName
     */
    @Nullable
    public static SlotType getSlotType(LivingEntity entity, String slotName){
        return getSlotTypes(entity.level()).get(slotName);
    }

    /**
     * Attempt to get the given SlotType based on the provided slotName
     */
    @Nullable
    public static SlotType getSlotType(Level level, String slotName){
        return getSlotTypes(level).get(slotName);
    }

    /**
     * Get all SlotTypes registered
     */
    public static Map<String, SlotType> getSlotTypes(Level level){
        return INSTANCE.getSlotTypes(level.isClientSide());
    }

    //--

    public final Map<String, SlotType> getSlotTypes(boolean isClientSide){
        return isClientSide ? client : server;
    }

    @ApiStatus.Internal
    public void setSlotType(Map<String, SlotType> slotTypes){
        this.client = ImmutableMap.copyOf(slotTypes);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        var uniqueSlots = new HashMap<String, SlotBuilder>();

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

                return new SlotTypeReference(name);
            });
        } catch (Exception e) {
            LOGGER.error("[SlotTypeLoader]: Error occurred when trying to gather unique slots though code!", e);
        }

        var builders = new HashMap<String, SlotBuilder>();

        builders.putAll(uniqueSlots);

        for (var resourceEntry : data.entrySet()) {
            var location = resourceEntry.getKey();
            var jsonObject = resourceEntry.getValue();

            if(!AccessoriesInternals.isValidOnConditions(jsonObject, this.directory, location, null)) continue;

            var pathParts = location.getPath().split("/");

            String slotName = pathParts[pathParts.length - 1];
            String namespace = pathParts.length > 1 ? pathParts[0] + ":" : "";

            var slotBuilder = builders.computeIfAbsent(namespace + slotName, SlotBuilder::new);

            slotBuilder.icon(safeHelper((object, s) -> ResourceLocation.tryParse(GsonHelper.getAsString(object, s)), jsonObject, "icon", location));

            slotBuilder.order(this.<Integer>safeHelper(GsonHelper::getAsInt, jsonObject, "order", location));

            if(ExtraSlotTypeProperties.getProperty(slotBuilder.name, false).allowResizing()){
                var amount = this.safeHelper(GsonHelper::getAsInt, jsonObject, "amount", location);

                if(amount != null) {
                    var operation = this.safeHelper((jsonObject1, s) -> {
                        try {
                            return OperationType.valueOf(GsonHelper.getAsString(jsonObject1, s).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }, jsonObject, "operation", null, location);

                    if(operation != null) {
                        switch (operation) {
                            case SET -> slotBuilder.amount(amount);
                            case ADD -> slotBuilder.addAmount(amount);
                            case SUB -> slotBuilder.subtractAmount(amount);
                        }
                    } else {
                        LOGGER.error("Unable to understand the passed operation for the given slot type file! [Location: {}, Operation: {}]", location, operation);
                    }
                }
            }

            if(!ExtraSlotTypeProperties.getProperty(slotBuilder.name, false).strictMode()) {
                var validators = safeHelper(GsonHelper::getAsJsonArray, jsonObject, "validators", new JsonArray(), location);

                decodeJsonArray(validators, "validator", location, element -> ResourceLocation.tryParse(element.getAsString()), slotBuilder::validator);
            }

            slotBuilder.dropRule(this.safeHelper((object, s) -> DropRule.valueOf(GsonHelper.getAsString(object, s)), jsonObject, "drop_rule", location));

            builders.put(slotBuilder.name, slotBuilder);
        }

        var tempMap = new HashMap<String, SlotType>();

        for (AccessoriesConfig.SlotAmountModifier modifier : Accessories.getConfig().modifiers) {
            var builder = builders.getOrDefault(modifier.slotType, null);

            if(builder == null) continue;

            builder.addAmount(modifier.amount);
        }

        uniqueSlots.forEach((s, slotBuilder) -> tempMap.put(s, slotBuilder.create()));
        builders.forEach((s, slotBuilder) -> {
            if(s.equals("any")) return;

            tempMap.put(s, slotBuilder.create());
        });

        this.server = ImmutableMap.copyOf(tempMap);
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
            this.icon = value;
            return this;
        }

        public SlotBuilder order(Integer value){
            this.order = value;
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
            this.dropRule = value;
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
