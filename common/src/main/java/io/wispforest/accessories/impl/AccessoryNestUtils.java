package io.wispforest.accessories.impl;

import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryNestContainerContents;
import io.wispforest.accessories.api.core.AccessoryNest;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
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

        if (accessory instanceof AccessoryNest && value == null) {
            var data = getData(stack);

            if (data != null) {
                var innerStacks = data.accessories();

                for (int i = 0; i < innerStacks.size(); i++) {
                    var innerStack = innerStacks.get(i);

                    if (innerStack.isEmpty()) continue;

                    value = recursiveStackHandling(innerStack, SlotPath.cloneWithInnerIndex(reference, i), function);

                    if(value != null) break;
                }

                if (reference instanceof SlotReference ref) {
                    AccessoryNest.checkIfChangesOccurred(stack, ref.entity(), data);
                }
            }
        }

        return value;
    }

    public static <S extends SlotPath> void recursiveStackConsumption(ItemStack stack, S reference, BiConsumer<ItemStack, S> consumer) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        consumer.accept(stack, reference);

        if (!(accessory instanceof AccessoryNest)) return;

        var data = getData(stack);

        if (data != null) {
            var innerStacks = data.accessories();

            for (int i = 0; i < innerStacks.size(); i++) {
                var innerStack = innerStacks.get(i);

                if (innerStack.isEmpty()) continue;

                recursiveStackConsumption(innerStack, SlotPath.cloneWithInnerIndex(reference, i), consumer);
            }

            if (reference instanceof SlotReference ref) {
                AccessoryNest.checkIfChangesOccurred(stack, ref.entity(), data);
            }
        }
    }

    public static <S extends SlotPath> void recursiveStackConsumption(ItemStack stack, Consumer<ItemStack> consumer) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        consumer.accept(stack);

        if (!(accessory instanceof AccessoryNest)) return;

        var data = getData(stack);

        if (data != null) {
            var innerStacks = data.accessories();

            for (var innerStack : innerStacks) {
                if (innerStack.isEmpty()) continue;

                recursiveStackConsumption(innerStack, consumer);
            }
        }
    }

}
