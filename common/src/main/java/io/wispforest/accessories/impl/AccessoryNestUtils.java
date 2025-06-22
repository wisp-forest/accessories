package io.wispforest.accessories.impl;

import io.wispforest.accessories.api.core.AccessoryNest;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryNestContainerContents;
import io.wispforest.accessories.api.slot.SlotPath;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class AccessoryNestUtils {

    @Nullable
    public static AccessoryNestContainerContents getData(ItemStack stack){
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        if(!(accessory instanceof AccessoryNest)) return null;

        return stack.get(AccessoriesDataComponents.NESTED_ACCESSORIES);
    }

    public static <T, S extends SlotPath> @Nullable T recursiveStackHandling(ItemStack stack, S reference, BiFunction<ItemStack, S, @Nullable T> function) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        var value = function.apply(stack, reference);

        if (accessory instanceof AccessoryNest holdable && value == null) {
            var innerStacks = holdable.getInnerStacks(stack);

            for (int i = 0; i < innerStacks.size(); i++) {
                var innerStack = innerStacks.get(i);

                if (innerStack.isEmpty()) continue;

                value = recursiveStackHandling(innerStack, SlotPath.cloneWithInnerIndex(reference, i), function);

                if(value != null) return value;
            }
        }

        return value;
    }

    public static <S extends SlotPath> void recursiveStackConsumption(ItemStack stack, S reference, BiConsumer<ItemStack, S> consumer) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        consumer.accept(stack, reference);

        if (!(accessory instanceof AccessoryNest holdable)) return;

        var innerStacks = holdable.getInnerStacks(stack);

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            if (innerStack.isEmpty()) continue;

            recursiveStackConsumption(innerStack, SlotPath.cloneWithInnerIndex(reference, i), consumer);
        }
    }

    public static <S extends SlotPath> void recursiveStackConsumption(ItemStack stack, Consumer<ItemStack> consumer) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        consumer.accept(stack);

        if (!(accessory instanceof AccessoryNest holdable)) return;

        var innerStacks = holdable.getInnerStacks(stack);

        for (ItemStack innerStack : innerStacks) {
            if (innerStack.isEmpty()) continue;

            recursiveStackConsumption(innerStack, consumer);
        }
    }

}
