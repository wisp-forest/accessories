package dev.emi.trinkets;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.intellij.lang.annotations.Identifier;

public class TrinketModifiers {
    //internalizes getTrinket and slotIdentifier, both which typically are generated just before the modifiers call anyway
    public static Multimap<Holder<Attribute>, AttributeModifier> get(ItemStack stack, SlotReference slot, LivingEntity entity){
        Multimap<Holder<Attribute>, AttributeModifier> map = TrinketsApi.getTrinket(stack.getItem()).getModifiers(stack, slot, entity, SlotAttributes.getIdentifier(slot));
        if (stack.has(TrinketsAttributeModifiersComponent.TYPE)) {
            for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
                map.put(entry.attribute(), entry.modifier());
            }
        }
        return map;
    }

    //overload if a custom method for retrieving the trinket is used. Also exposes the slotIdentifier if custom on that is needed
    public static Multimap<Holder<Attribute>, AttributeModifier> get(Trinket trinket, ItemStack stack, SlotReference slot, LivingEntity entity, ResourceLocation slotIdentifier){
        Multimap<Holder<Attribute>, AttributeModifier> map = trinket.getModifiers(stack, slot, entity, slotIdentifier);
        if (stack.has(TrinketsAttributeModifiersComponent.TYPE)) {
            for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
                if (entry.slot().isEmpty() || entry.slot().get().equals(slot.inventory().getSlotType().getId())) {
                    map.put(entry.attribute(), entry.modifier());
                }
            }
        }
        return map;
    }
}
