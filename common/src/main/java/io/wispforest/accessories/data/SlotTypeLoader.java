package io.wispforest.accessories.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SlotType;
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

    public final Map<String, SlotType> getSlotTypes(Level level){
        return getSlotTypes(level.isClientSide());
    }

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

        for (var resourceEntry : data.entrySet()) {
            var location = resourceEntry.getKey();
            var jsonObject = resourceEntry.getValue();

            if(!AccessoriesAccess.getInternal().isValidOnConditions(jsonObject)) continue;

            if(!location.getNamespace().equals(Accessories.MODID)) continue;

            var slotName = location.getPath();

            var slotBuilder = new SlotBuilder(slotName);

            slotBuilder.icon(safeHelper((object, s) -> ResourceLocation.tryParse(GsonHelper.getAsString(object, s)), jsonObject, "icon", location));

            slotBuilder.order(this.<Integer>safeHelper(GsonHelper::getAsInt, jsonObject, "order", location));

            var amount = this.safeHelper(GsonHelper::getAsInt, jsonObject, "amount", location);

            var operation = this.safeHelper((jsonObject1, s) -> GsonHelper.getAsString(jsonObject1, s).toLowerCase(), jsonObject, "operation", "set", location);

            if("set".equals(operation)) {
                if(amount != null) slotBuilder.amount(amount);
            } else if ("add".equals(operation)) {
                if(amount == null) amount = 1;

                slotBuilder.addAmount(amount);
            } else if ("sub".equals(operation)) {
                if(amount == null) amount = 1;

                slotBuilder.subtractAmount(amount);
            }

            var validators = safeHelper(GsonHelper::getAsJsonArray, jsonObject, "validator_predicates", new JsonArray(), location);

            decodeJsonArray(validators, "validator", location, element -> ResourceLocation.tryParse(element.getAsString()), slotBuilder::validator);

            slotBuilder.dropRule(this.safeHelper((object, s) -> DropRule.valueOf(GsonHelper.getAsString(object, s)), jsonObject, "drop_rule", location));

            var slotType = slotBuilder.create();

            if(server.containsKey(slotType.name())){
                LOGGER.warn("Found duplicate slotType with the same name, not registering newly made type! [Location: " + location + "]");
            } else {
                server.put(slotType.name(), slotType);
            }
        }
    }

    public static class SlotBuilder {
        private final String name;
        private ResourceLocation icon = null;
        private Integer order = null;
        private Integer amount = null;
        private final Set<ResourceLocation> validators = new HashSet<>();
        private DropRule dropRule = null;

        public SlotBuilder(String name){
            this.name = name;
        }

        public SlotBuilder icon(ResourceLocation value){
            this.icon = value;
            return this;
        }

        public SlotBuilder order(int value){
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
                    Optional.ofNullable(icon).orElse(SlotType.EMPTY_SLOT_LOCATION),
                    Optional.ofNullable(order).orElse(1000),
                    Optional.ofNullable(amount).map(i -> Math.max(i, 0)).orElse(1),
                    validators,
                    Optional.ofNullable(dropRule).orElse(DropRule.DEFAULT)
            );
        }
    }
}
