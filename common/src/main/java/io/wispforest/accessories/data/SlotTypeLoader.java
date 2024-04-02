package io.wispforest.accessories.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.SlotTypeReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.impl.SlotTypeImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import java.util.*;

public class SlotTypeLoader extends ReplaceableJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();

    public static final SlotTypeLoader INSTANCE = new SlotTypeLoader();

    protected SlotTypeLoader() {
        super(GSON, LOGGER, "accessories/slot");
    }

    private final Map<String, SlotType> server = new HashMap<>();
    private final Map<String, SlotType> client = new HashMap<>();

    //--

    /**
     * Attempt to get the given SlotType based on the provided slotName
     */
    public static Optional<SlotType> getSlotType(Level level, String slotName){
        return Optional.ofNullable(getSlotTypes(level).get(slotName));
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
        this.client.clear();
        this.client.putAll(slotTypes);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        server.clear();

        var additionalModifiers = new HashMap<String, Integer>();

        for (AccessoriesConfig.SlotAmountModifier modifier : Accessories.getConfig().modifiers) {
            additionalModifiers.put(modifier.slotType, modifier.amount);
        }

        var uniqueSlots = new HashMap<String, SlotBuilder>();

        UniqueSlotHandling.EVENT.invoker().registerSlots((location, integer, slotPredicate, types) -> {
            var name = location.toString();

            var builder = new SlotBuilder(name);

            builder.amount(integer);

            uniqueSlots.put(name, builder);

            if(slotPredicate != null) {
                builder.validator(slotPredicate);
            } else {
                builder.validator(Accessories.of("tag"));
            }

            // TODO: Should be replace with throw?

            return new SlotTypeReference(name);
        });

        var builders = new HashMap<String, SlotBuilder>();

        for (var resourceEntry : data.entrySet()) {
            var location = resourceEntry.getKey();
            var jsonObject = resourceEntry.getValue();

            if(!AccessoriesInternals.isValidOnConditions(jsonObject)) continue;

            var isShared = location.getNamespace().equals(Accessories.MODID);

            if(!isShared && !uniqueSlots.containsKey(location.toString())) {
                LOGGER.error("A Unique slot was attempted to be adjust though datapack but was not found to register in the UniqueSlotHandling event, such will be ignored");

                return;
            }

            var pathParts = location.getPath().split("/");

            String name = pathParts[pathParts.length - 1];

            var slotBuilder = isShared ? new SlotBuilder(name) : uniqueSlots.remove(location.getNamespace() + ":" + name);

            slotBuilder.icon(safeHelper((object, s) -> ResourceLocation.tryParse(GsonHelper.getAsString(object, s)), jsonObject, "icon", location));

            slotBuilder.order(this.<Integer>safeHelper(GsonHelper::getAsInt, jsonObject, "order", location));

            if(isShared) {
                var amount = this.safeHelper(GsonHelper::getAsInt, jsonObject, "amount", location);

                var operation = this.safeHelper((jsonObject1, s) -> GsonHelper.getAsString(jsonObject1, s).toLowerCase(), jsonObject, "operation", "set", location);

                if ("set".equals(operation)) {
                    if (amount != null) slotBuilder.amount(amount);
                } else if ("add".equals(operation)) {
                    if (amount == null) amount = 1;

                    slotBuilder.addAmount(amount);
                } else if ("sub".equals(operation)) {
                    if (amount == null) amount = 1;

                    slotBuilder.subtractAmount(amount);
                }

                slotBuilder.addAmount(additionalModifiers.getOrDefault(slotBuilder.name, 0));

                var validators = safeHelper(GsonHelper::getAsJsonArray, jsonObject, "validator_predicates", new JsonArray(), location);

                decodeJsonArray(validators, "validator", location, element -> ResourceLocation.tryParse(element.getAsString()), slotBuilder::validator);
            }

            slotBuilder.dropRule(this.safeHelper((object, s) -> DropRule.valueOf(GsonHelper.getAsString(object, s)), jsonObject, "drop_rule", location));

            if(server.containsKey(slotBuilder.name)){
                LOGGER.warn("Found duplicate slotType with the same name, not registering newly made type! [Location: " + location + "]");

                return;
            }

            builders.put(slotBuilder.name, slotBuilder);
        }

        uniqueSlots.forEach((s, slotBuilder) -> server.put(s, slotBuilder.create()));

        builders.forEach((s, slotBuilder) -> server.put(s, slotBuilder.create()));
    }

    public static class SlotBuilder {
        private final String name;
        private ResourceLocation icon = null;
        private Integer order = null;
        private Integer amount = null;
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
            this.amount = value;
            return this;
        }

        public SlotBuilder addAmount(int value){
            this.amount += value;
            return this;
        }

        public SlotBuilder subtractAmount(int value){
            this.amount -= value;
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
            if(validators.isEmpty()) {
                validators.add(Accessories.of("tag"));
                validators.add(Accessories.of("compound"));
            }

            return new SlotTypeImpl(
                    name,
                    alternativeTranslation,
                    Optional.ofNullable(icon).orElse(SlotType.EMPTY_SLOT_LOCATION),
                    Optional.ofNullable(order).orElse(1000),
                    Optional.ofNullable(amount).map(i -> Math.max(i, 0)).orElse(1),
                    validators,
                    Optional.ofNullable(dropRule).orElse(DropRule.DEFAULT)
            );
        }
    }
}
