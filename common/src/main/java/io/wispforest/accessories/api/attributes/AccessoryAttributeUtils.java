package io.wispforest.accessories.api.attributes;

import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryItemAttributeModifiers;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AccessoryAttributeUtils {

    public static void addAttribute(ItemStack stack, String slotName, Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        stack.update(
                AccessoriesDataComponents.ATTRIBUTES,
                new AccessoryItemAttributeModifiers(List.of(), true),
                modifiers -> modifiers.withModifierAdded(attribute, new AttributeModifier(location, amount, operation), slotName, isStackable)
        );
    }

    public static void removeAttribute(ItemStack stack, Holder<Attribute> attribute, ResourceLocation location) {
        stack.update(
                AccessoriesDataComponents.ATTRIBUTES,
                new AccessoryItemAttributeModifiers(List.of(), true),
                modifiers -> modifiers.withoutModifier(attribute, location)
        );
    }
}
