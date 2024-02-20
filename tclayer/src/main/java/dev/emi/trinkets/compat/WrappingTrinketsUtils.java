package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketConstants;
import io.wispforest.accessories.api.AccessoriesAPI;

import java.util.Optional;

public class WrappingTrinketsUtils {

    public static Optional<SlotReference> createReference(io.wispforest.accessories.api.slot.SlotReference slotReference){
        try {
            var capability = AccessoriesCapability.get(slotReference.entity());

            if(capability.isEmpty()) return Optional.empty();

            var curiosHandler = capability.get().getContainers().get(TrinketConstants.trinketsToAccessories(slotReference.slotName()));

            var slotType = SlotTypeLoader.getSlotType(slotReference.entity().level(), curiosHandler.getSlotName()).get();

            var trinketInv = new WrappedTrinketInventory(new LivingEntityTrinketComponent(capability.get()), curiosHandler, slotType);

            return Optional.of(new SlotReference(trinketInv, slotReference.slot()));
        } catch (Exception e){
            return Optional.empty();
        }
    }
}
