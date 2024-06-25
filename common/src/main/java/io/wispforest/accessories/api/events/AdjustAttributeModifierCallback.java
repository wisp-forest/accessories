package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Event callback used to adjust the given attributes being evaluated within {@link AccessoriesAPI#getAttributeModifiers(ItemStack, LivingEntity, String, int)}
 * call to which is fired after evaluation of NBT and Accessories own attributes
 */
public interface AdjustAttributeModifierCallback {

    Event<AdjustAttributeModifierCallback> EVENT = EventFactory.createArrayBacked(AdjustAttributeModifierCallback.class,
            (invokers) -> (stack, reference, builder) -> {
                AccessoryNestUtils.recursiveStackConsumption(stack, reference, (stack1, reference1) -> {
                    var innerBuilder = new AccessoryAttributeBuilder(reference1);

                    for (var invoker : invokers) invoker.adjustAttributes(stack1, reference1, innerBuilder);

                    builder.addFrom(innerBuilder);
                });
            }
    );

    /**
     * @param stack     The specific stack being evaluated
     * @param reference The reference to the specific location within the Accessories Inventory
     * @param builder   The builder containing the to be applied attributes modifications
     */
    void adjustAttributes(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder);
}
