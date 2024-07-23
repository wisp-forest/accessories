package io.wispforest.accessories.api.events;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import oshi.annotation.concurrent.Immutable;

/**
 * Event callback used to allow or denied the ability to equip a given accessory for the given referenced slot
 * type and entity.
 * <p>
 * Fired in {@link AccessoriesAPI#canEquip(ItemStack, SlotReference)}
 */
public interface CanEquipCallback {

    Event<CanEquipCallback> EVENT = EventFactory.createArrayBacked(CanEquipCallback.class,
            (invokers) -> (stack, reference) -> {
                var result =  AccessoryNestUtils.recursiveStackHandling(stack, reference, (stack1, reference1) -> {
                    TriState finalResult = null;

                    for (var invoker : invokers) {
                        var returnResult = invoker.canEquip(stack1, reference1);

                        if(returnResult.equals(TriState.FALSE)) {
                            finalResult = returnResult;

                            break;
                        }
                    }

                    return finalResult;
                });

                return result != null ? result : TriState.DEFAULT;
            }
    );

    /**
     * @param stack     The specific stack being evaluated
     * @param reference The reference to the specific location within the Accessories Inventory
     * @return If the given stack can be equipped
     */
    TriState canEquip(ItemStack stack, SlotReference reference);
}
