package io.wispforest.accessories.api.core;

import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryNestContainerContents;
import io.wispforest.accessories.api.events.SlotStateChange;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.pond.stack.PatchedDataComponentMapExtension;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

///
/// General utility for recursively handling and/or consuming the nest and/or their children entries. Basically
/// a class for helping with unpacking data and attempting to interact with it safely
///
public class AccessoryNestUtils {

    @Nullable
    public static AccessoryNestContainerContents getData(ItemStack stack){
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        if(!(accessory instanceof AccessoryNest)) return null;

        return stack.get(AccessoriesDataComponents.NESTED_ACCESSORIES);
    }

    public static boolean checkIfChangesOccurred(ItemStack holderStack, @Nullable LivingEntity livingEntity, AccessoryNestContainerContents data) {
        boolean hasChangeOccurred = false;

        var accessories = data.accessories();

        var isNest = AccessoryNest.isNest(holderStack);

        for (int i = 0; i < accessories.size(); i++) {
            var stack = accessories.get(i);

            if(stack.getComponents() instanceof PatchedDataComponentMapExtension extension && extension.accessories$hasChanged()){
                hasChangeOccurred = true;
            } else if(data.slotChanges().containsKey(i)) {
                hasChangeOccurred = true;
            } else if(isNest) {
                var innerData = AccessoryNestUtils.getData(stack);

                if(innerData != null) {
                    hasChangeOccurred = checkIfChangesOccurred(stack, livingEntity, innerData);

                    if(hasChangeOccurred) break;
                }
            }

            if(hasChangeOccurred) {
                data.slotChanges().putIfAbsent(i, SlotStateChange.MUTATION);
            }
        }

        if(hasChangeOccurred && isNest) {
            var nest = (AccessoryNest) AccessoryRegistry.getAccessoryOrDefault(holderStack);

            holderStack.set(AccessoriesDataComponents.NESTED_ACCESSORIES, data);

            nest.onStackChanges(holderStack, data, livingEntity);
        }

        return hasChangeOccurred;
    }

    //--

    ///
    /// Runs `NestedAccessoryFunction` for the root `stack` and any children entries recursively looking for other
    /// [AccessoryNest]s entries **returning** either a value of type [T] or null if defaulted
    ///
    /// @param stack      Stack which has a [AccessoryNest] bound to it
    /// @param reference  Reference to a position within a [AccessoriesStorage][AccessoriesStorage]
    /// @param function   Function to execute for each stack recursively found i.e. either a [Accessory] or another [AccessoryNest]
    ///
    public static <T, S extends SlotPath> @Nullable T recursivelyHandle(ItemStack stack, S reference, PathedStackFunction<S, T> function) {
        return recursivelyHandle(stack, reference, (PathedAccessoryFunction<S, T>) function);
    }

    ///
    /// Runs `NestedAccessoryFunction` for the root `stack` and any children entries recursively looking for other
    /// [AccessoryNest]s entries **returning** either a value of type [T] or null if defaulted
    /// 
    /// @param stack      Stack which has a [AccessoryNest] bound to it
    /// @param reference  Reference to a position within a [AccessoriesStorage][AccessoriesStorage]
    /// @param function   Function to execute for each stack recursively found i.e. either a [Accessory] or another [AccessoryNest]
    ///
    public static <T, S extends SlotPath> @Nullable T recursivelyHandle(ItemStack stack, S reference, PathedAccessoryFunction<S, T> function) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        var value = function.handle(accessory, stack, reference);

        if (accessory instanceof AccessoryNest && function.isDefaulted(value)) {
            value = handleEntries(stack, reference, (innerStack, innerRef) -> recursivelyHandle(innerStack, innerRef, function));
        }

        return value;
    }

    public static <T, S extends SlotPath> @Nullable T handleEntries(ItemStack stack, S reference, PathedAccessoryFunction<S, T> function) {
        return handleEntries(stack, reference, (innerStack, innerRef) -> function.handle(AccessoryRegistry.getAccessoryOrDefault(innerStack), innerStack, innerRef));
    }

    public static <T, S extends SlotPath> @Nullable T handleEntries(ItemStack stack, S reference, PathedStackFunction<S, T> function) {
        T value = null;

        var data = getData(stack);

        if (data != null) {
            value = data.iterateStacks(
                (i, innerStack) -> function.handle(innerStack, SlotPath.cloneWithInnerIndex(reference, i)),
                function
            );

            if (reference instanceof SlotReference ref) {
                checkIfChangesOccurred(stack, ref.entity(), data);
            }
        }

        return value;
    }

    //--

    public static <S extends SlotPath> void recursivelyConsume(ItemStack stack, S reference, PathedStackConsumer<S> consumer) {
        recursivelyConsume(stack, reference, (PathedAccessoryConsumer<S>) consumer);
    }

    public static <S extends SlotPath> void recursivelyConsume(ItemStack stack, S reference, PathedAccessoryConsumer<S> consumer) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        consumer.handle(accessory, stack, reference);

        if (!(accessory instanceof AccessoryNest)) return;

        consumeEntries(stack, reference, (innerStack, innerRef) -> recursivelyConsume(innerStack, innerRef, consumer));
    }

    public static <S extends SlotPath> void consumeEntries(ItemStack stack, S reference, PathedAccessoryConsumer<S> consumer) {
        consumeEntries(stack, reference, (innerStack, innerRef) -> consumer.handle(AccessoryRegistry.getAccessoryOrDefault(innerStack), innerStack, innerRef));
    }

    public static <S extends SlotPath> void consumeEntries(ItemStack stack, S reference, PathedStackConsumer<S> consumer) {
        var data = getData(stack);

        if (data == null) return;

        data.iterateStacks((i, innerStack) -> {
            consumer.handle(innerStack, SlotPath.cloneWithInnerIndex(reference, i));
        });

        if (!(reference instanceof SlotReference ref)) return;

        checkIfChangesOccurred(stack, ref.entity(), data);
    }

    //--

    public static <T> @Nullable T recursivelyHandle(ItemStack stack, StackFunction<T> function) {
        return recursivelyHandle(stack, (accessory, innerStack) -> function.handle(innerStack));
    }

    public static <T> @Nullable T recursivelyHandle(ItemStack stack, AccessoryFunction<T> function) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        var value = function.handle(accessory, stack);

        if (accessory instanceof AccessoryNest && function.isDefaulted(value)) {
            value = handleEntries(stack, innerStack -> recursivelyHandle(innerStack, function), function);
        }

        return value;
    }

    public static <T> @Nullable T handleEntries(ItemStack stack, AccessoryFunction<T> function) {
        return handleEntries(stack, (innerStack) -> function.handle(AccessoryRegistry.getAccessoryOrDefault(innerStack), innerStack), function);
    }

    public static <T> @Nullable T handleEntries(ItemStack stack, StackFunction<T> function) {
        return handleEntries(stack, function, (DefaultBehavior<T>) DefaultBehavior.INSTANCE);
    }

    public static <T> @Nullable T handleEntries(ItemStack stack, StackFunction<T> function, DefaultBehavior<T> behavior) {
        var data = getData(stack);

        if (data == null) return null;

        T value = null;

        for (var innerStack : data.accessories()) {
            if (innerStack.isEmpty()) continue;

            value = function.handle(innerStack);

            if (!behavior.isDefaulted(value)) break;
        }

        return value;
    }

    //--

    public static void recursivelyConsume(ItemStack stack, StackConsumer consumer) {
        recursivelyConsume(stack, (accessory, innerStack) -> consumer.handle(innerStack));
    }

    public static void recursivelyConsume(ItemStack stack, AccessoryConsumer consumer) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(stack);

        consumer.handle(accessory, stack);

        if (!(accessory instanceof AccessoryNest)) return;

        consumeEntries(stack, innerStack -> recursivelyConsume(innerStack, consumer));
    }

    public static void consumeEntries(ItemStack stack, AccessoryConsumer consumer) {
        consumeEntries(stack, (innerStack) -> consumer.handle(AccessoryRegistry.getAccessoryOrDefault(innerStack), innerStack));
    }

    public static void consumeEntries(ItemStack stack, StackConsumer consumer) {
        var data = getData(stack);

        if (data == null) return;

        data.accessories().forEach(innerStack -> {
            if (!innerStack.isEmpty()) consumer.handle(innerStack);
        });
    }

    //--

    // TODO: MOVE ALL INTERFACES TO SUB FOLDER

    // TODO: RENAME TO SOMETHING ELSE BETTER?
    public interface DefaultBehavior<T> {
        static DefaultBehavior INSTANCE = new DefaultBehavior() {};

        default boolean isDefaulted(@Nullable T t) {
            return t == null;
        }
    }

    public interface PathedStackFunction<S extends SlotPath, T> extends PathedAccessoryFunction<S, T> {
        @Override
        default @Nullable T handle(Accessory accessory, ItemStack innerStack, S innerRef) {
            return handle(innerStack, innerRef);
        }

        @Nullable T handle(ItemStack innerStack, S innerRef);
    }

    public interface PathedAccessoryFunction<S extends SlotPath, T> extends DefaultBehavior<T> {
        @Nullable T handle(Accessory accessory, ItemStack innerStack, S innerRef);
    }

    public interface PathedStackConsumer<S extends SlotPath> extends PathedAccessoryConsumer<S> {
        @Override
        default void handle(Accessory accessory, ItemStack innerStack, S innerRef) {
            handle(innerStack, innerRef);
        }

        void handle(ItemStack innerStack, S innerRef);
    }

    public interface PathedAccessoryConsumer<S extends SlotPath> {
        void handle(Accessory accessory, ItemStack innerStack, S innerRef);
    }

    //--

    public interface StackFunction<T> extends AccessoryFunction<T> {
        @Override
        default @Nullable T handle(Accessory accessory, ItemStack innerStack) {
            return handle(innerStack);
        }

        @Nullable T handle(ItemStack innerStack);
    }

    public interface AccessoryFunction<T> extends DefaultBehavior<T> {
        @Nullable T handle(Accessory accessory, ItemStack innerStack);
    }

    public interface StackConsumer extends AccessoryConsumer {
        @Override
        default void handle(Accessory accessory, ItemStack innerStack) {
            handle(innerStack);
        }

        void handle(ItemStack innerStack);
    }

    public interface AccessoryConsumer {
        void handle(Accessory accessory, ItemStack innerStack);
    }
}
