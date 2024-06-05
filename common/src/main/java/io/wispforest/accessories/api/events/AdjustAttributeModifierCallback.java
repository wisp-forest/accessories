package io.wispforest.accessories.api.events;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.events.extra.AllowWalkingOnSnow;
import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PowderSnowBlock;

import java.util.UUID;

/**
 * Event callback used to adjust the given attributes being evaluated within {@link AccessoriesAPI#getAttributeModifiers(ItemStack, LivingEntity, String, int, UUID)}
 * call to which is fired after evaluation of NBT and Accessories own attributes
 */
public interface AdjustAttributeModifierCallback {

    Event<AdjustAttributeModifierCallback> EVENT = EventFactory.createArrayBacked(AdjustAttributeModifierCallback.class,
            (invokers) -> (stack, reference, uuid, modifiers) -> {
                AccessoryNestUtils.recursiveStackConsumption(stack, reference, (stack1, reference1) -> {
                    for (var invoker : invokers) invoker.adjustAttributes(stack1, reference1,  uuid, modifiers);
                });
            }
    );

    /**
     * @param stack     The specific stack being evaluated
     * @param reference The reference to the specific location within the Accessories Inventory
     * @param uuid      The UUID create from the given referenced slot type and index
     * @param modifiers The current attribute modifiers
     */
    void adjustAttributes(ItemStack stack, SlotReference reference, UUID uuid, Multimap<Attribute, AttributeModifier> modifiers);
}
