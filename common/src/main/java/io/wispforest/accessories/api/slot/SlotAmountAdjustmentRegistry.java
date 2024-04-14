package io.wispforest.accessories.api.slot;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.event.EventUtils;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * An API for adjusting a given slot by setting a target at which the slot amount should attempt to be set at
 */
public class SlotAmountAdjustmentRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SlotAmountAdjustmentRegistry INSTANCE = new SlotAmountAdjustmentRegistry();

    private final Map<String, Map<ResourceLocation, SlotAmountAdjustment>> allSuppliers = new HashMap<>();

    /**
     * Method used to register an amount at which a given slot's base size should be based on the passed {@link LivingEntity}
     *
     * @param slotType Targeted SlotType to apply such function to
     * @param location The given location for the adjuster
     * @param targetFunc The passed amount adjustment function
     * @return A consumer used to update a given {@link LivingEntity}'s slot amount
     */
    public Consumer<LivingEntity> addAmountSupplier(SlotType slotType, ResourceLocation location, SlotAmountAdjustment targetFunc){
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

        container.markChanged();
        container.update();
    }

    public static int getAmount(SlotType slotType, LivingEntity livingEntity){
        var operations = INSTANCE.allSuppliers.getOrDefault(slotType.name(), Map.of()).values().stream()
                .map(targetFunc -> targetFunc.getAmount(livingEntity))
                .collect(Collectors.toSet());

        int amount = slotType.amount();

        for (var operation : operations) {
            if(!operation.type().equals(OperationType.SET)) continue;

            amount = operation.attemptOperation(amount);
        }

        for (var operation : operations) {
            if(operation.type().equals(OperationType.SET)) continue;

            amount = operation.attemptOperation(amount);
        }

        return amount;
    }

    public static void onReload(){
        INSTANCE.allSuppliers.clear();

        SlotAmountAdjustmentRegistry.AFTER_SLOT_LOAD.invoker().registerAdjustments(INSTANCE);
    }

    //--

    public interface SlotAmountAdjustment {
        ArithmeticOperation getAmount(LivingEntity livingEntity);
    }

    public record ArithmeticOperation(OperationType type, int amount){
        public int attemptOperation(int base) {
            return switch (type) {
                case ADD -> base + amount;
                case SUB -> base - amount;
                case SET -> amount;
            };
        }
    }

    public static final Event<SlotAdjustmentRegister> AFTER_SLOT_LOAD = EventUtils.createEventWithBus(SlotAdjustmentRegister.class, AccessoriesInternals::getBus, (iEventBus, invokers) -> instance -> {
        instance.allSuppliers.clear();

        for (var invoker : invokers) invoker.registerAdjustments(instance);

        iEventBus.ifPresent(eventBus -> eventBus.post(new SlotAdjustmentRegisterEvent(instance)));
    });

    public interface SlotAdjustmentRegister {
        void registerAdjustments(SlotAmountAdjustmentRegistry instance);
    }

    public static final class SlotAdjustmentRegisterEvent extends net.neoforged.bus.api.Event {
        private final SlotAmountAdjustmentRegistry instance;

        public SlotAdjustmentRegisterEvent(SlotAmountAdjustmentRegistry instance) {
            this.instance = instance;
        }

        public SlotAmountAdjustmentRegistry instance() {
            return instance;
        }
    }
}
