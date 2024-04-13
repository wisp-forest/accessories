package io.wispforest.accessories.api;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.event.EventUtils;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An API for adjusting a given slot by setting a target at which the slot amount should attempt to be set at
 */
public class SlotAmountAdjustments {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SlotAmountAdjustments INSTANCE = new SlotAmountAdjustments();

    private final Map<String, Map<ResourceLocation, SlotAmountTarget>> allSuppliers = new HashMap<>();

    /**
     * Method used to register an amount at which a given slot's base size should be based on the passed {@link LivingEntity}
     *
     * @param slotType Targeted SlotType to apply such function to
     * @param location The given location for the adjuster
     * @param targetFunc The passed amount adjustment function
     * @return A consumer used to update a given {@link LivingEntity}'s slot amount
     */
    public Consumer<LivingEntity> addAmountSupplier(SlotType slotType, ResourceLocation location, SlotAmountTarget targetFunc){
        var slotName = slotType.name();
        var slotSpecificSuppliers = allSuppliers.computeIfAbsent(slotName, s -> new HashMap<>());

        if(slotSpecificSuppliers.containsKey(location)){
            LOGGER.error("[SlotAmountAdjustments]: Unable to register a supplier for SlotType {} for the given location {} as it already exists!", slotName, location);
        }

        slotSpecificSuppliers.put(location, targetFunc);

        return livingEntity -> onSupplierChange(slotName, location, livingEntity);
    }

    private void onSupplierChange(String slotType, ResourceLocation location, LivingEntity livingEntity){
        var capability = AccessoriesCapability.get(livingEntity);

        if(capability == null) {
            LOGGER.error("[SlotAmountAdjustments]: Unable to update the given LivingEntity slot amount due to not having a AccessoriesCapability. [Slot: {}, Supplier: {}]", slotType, location);
            return;
        }

        var slot = SlotTypeLoader.getSlotType(livingEntity.level(), slotType);

        if(slot == null){
            LOGGER.error("[SlotAmountAdjustments]: Unable to find the given slotType within the Registry to adjust using the given supplier. [Slot: {}, Supplier: {}]", slotType, location);
            return;
        }

        var container = capability.tryAndGetContainer(slot);

        if(container == null){
            LOGGER.error("[SlotAmountAdjustments] Attempted to update a given slotType's amount but no container was present for the livingEntity. [Slot: {}, Supplier: {}]", slotType, location);
            return;
        }

        var targetFunc = INSTANCE.allSuppliers.get(slotType).getOrDefault(location, null);

        if(targetFunc == null){
            LOGGER.error("[SlotAmountAdjustments]: Attempted to update a given slotType's amount but the given ResourceLocation was not found within the map!. [Slot: {}, Supplier: {}]", slotType, location);
            return;
        }

        var slotAmount = targetFunc.getAmount(livingEntity);

        if(slotAmount > container.getSize()) {
            container.markChanged();
            container.update();
        }
    }

    public static int getAmount(SlotType slotType, LivingEntity livingEntity){
        int amount = 0;

        for (var targetFunc : INSTANCE.allSuppliers.getOrDefault(slotType.name(), Map.of()).values()) {
            var newAmount = targetFunc.getAmount(livingEntity);

            if(newAmount > amount) amount = newAmount;
        }

        return amount;
    }

    public static void onReload(){
        INSTANCE.allSuppliers.clear();

        SlotAmountAdjustments.EVENT.invoker().afterLoad(INSTANCE);
    }

    //--

    public interface SlotAmountTarget {
        int getAmount(LivingEntity livingEntity);
    }

    public static final Event<AfterSlotLoad> EVENT = EventUtils.createEventWithBus(AfterSlotLoad.class, AccessoriesInternals::getBus, (iEventBus, invokers) -> instance -> {
        instance.allSuppliers.clear();

        for (var invoker : invokers) invoker.afterLoad(instance);

        iEventBus.ifPresent(eventBus -> eventBus.post(new AfterSlotLoadEvent(instance)));
    });

    public interface AfterSlotLoad {
        void afterLoad(SlotAmountAdjustments instance);
    }

    public static final class AfterSlotLoadEvent extends net.neoforged.bus.api.Event {
        private final SlotAmountAdjustments instance;

        public AfterSlotLoadEvent(SlotAmountAdjustments instance) {
            this.instance = instance;
        }

        public SlotAmountAdjustments instance() {
            return instance;
        }
    }
}
