package io.wispforest.accessories.utils;

import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BaseContainer implements Container, StackedContentsCompatible {
    private final int size;
    private final NonNullList<ItemStack> items;

    @Nullable
    private EventStream<ContainerListener> onContainerChange = null;

    public BaseContainer(int size) {
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public BaseContainer(ItemStack... items) {
        this.size = items.length;
        this.items = NonNullList.of(ItemStack.EMPTY, items);
    }

    /**
     * Add a listener that will be notified when any item in this inventory is modified.
     */
    public EventSource.Subscription addListener(ContainerListener listener) {
        if (this.onContainerChange == null) {
            this.onContainerChange = new EventStream<>(invokers -> container -> invokers.forEach(listenerEntry -> listenerEntry.containerChanged(container)));
        }

        return this.onContainerChange.source().subscribe(listener);
    }

    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size()
            ? this.items.get(slot)
            : ItemStack.EMPTY;
    }

    public List<ItemStack> removeAllItems() {
        var list = this.items.stream().filter(itemStack -> !itemStack.isEmpty()).collect(Collectors.toList());

        this.clearContent();

        return list;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        var itemStack = ContainerHelper.removeItem(this.items, slot, amount);

        if (!itemStack.isEmpty()) this.setChanged();

        return itemStack;
    }

    public ItemStack removeItemType(Item item, int amount) {
        var itemStack = new ItemStack(item, 0);

        for (int i = this.size - 1; i >= 0; i--) {
            var itemStack2 = this.getItem(i);

            if (itemStack2.getItem().equals(item)) {
                int j = amount - itemStack.getCount();

                var itemStack3 = itemStack2.split(j);

                itemStack.grow(itemStack3.getCount());

                if (itemStack.getCount() == amount) break;
            }
        }

        if (!itemStack.isEmpty()) this.setChanged();

        return itemStack;
    }

    public ItemStack addItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        var itemStack = stack.copy();
        this.moveItemToOccupiedSlotsWithSameType(itemStack);

        if (itemStack.isEmpty()) return ItemStack.EMPTY;

        this.moveItemToEmptySlots(itemStack);
        return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
    }

    public boolean canAddItem(ItemStack stack) {
        boolean bl = false;

        for (var itemStack : this.items) {
            if (itemStack.isEmpty() || ItemStack.isSameItemSameComponents(itemStack, stack) && itemStack.getCount() < itemStack.getMaxStackSize()) {
                bl = true;
                break;
            }
        }

        return bl;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var itemStack = this.items.get(slot);

        if (itemStack.isEmpty()) return ItemStack.EMPTY;

        this.items.set(slot, ItemStack.EMPTY);

        return itemStack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);

        stack.limitSize(this.getMaxStackSize(stack));

        this.setChanged();
    }

    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) return false;
        }

        return true;
    }

    @Override
    public void setChanged() {
        if (this.onContainerChange != null) {
            this.onContainerChange.sink().containerChanged(this);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public void fillStackedContents(StackedItemContents stackedItemContents) {
        for (var itemStack : this.items) stackedItemContents.accountStack(itemStack);
    }

    public String toString() {
        return this.items.stream()
            .filter(itemStack -> !itemStack.isEmpty())
            .toList()
            .toString();
    }

    private void moveItemToEmptySlots(ItemStack stack) {
        for (int i = 0; i < this.size; i++) {
            var itemStack = this.getItem(i);

            if (itemStack.isEmpty()) {
                this.setItem(i, stack.copyAndClear());
                return;
            }
        }
    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack stack) {
        for (int i = 0; i < this.size; i++) {
            var itemStack = this.getItem(i);

            if (ItemStack.isSameItemSameComponents(itemStack, stack)) {
                this.moveItemsBetweenStacks(stack, itemStack);

                if (stack.isEmpty()) return;
            }
        }
    }

    private void moveItemsBetweenStacks(ItemStack stack, ItemStack other) {
        int i = this.getMaxStackSize(other);
        int j = Math.min(stack.getCount(), i - other.getCount());
        if (j > 0) {
            other.grow(j);
            stack.shrink(j);
            this.setChanged();
        }
    }

    public final void saveAllItems(ValueOutput valueOutput) {
        var typedOutputList = valueOutput.list("Items", ItemStackWithSlot.CODEC);

        saveItemsToList(typedOutputList);
    }

    public final void loadAllItems(ValueInput valueInput) {
        loadItemsFromList(valueInput.listOrEmpty("Items", ItemStackWithSlot.CODEC));
    }

    public void saveItemsToList(ValueOutput.TypedOutputList<ItemStackWithSlot> valueOutput) {
        saveItemsToList().forEach(valueOutput::add);
    }

    public void loadItemsFromList(ValueInput.TypedInputList<ItemStackWithSlot> valueInput) {
        loadItemsFromList(valueInput.stream().toList());
    }

    //--

    public List<ItemStackWithSlot> saveItemsToList() {
        var slottedStacks = new ArrayList<ItemStackWithSlot>();

        for (int i = 0; i < items.size(); i++) {
            var itemStack = items.get(i);
            if (!itemStack.isEmpty()) {
                slottedStacks.add(new ItemStackWithSlot(i, itemStack));
            }
        }

        return slottedStacks;
    }

    public void loadItemsFromList(Collection<ItemStackWithSlot> slottedStacks) {
        for (var slottedStack : slottedStacks) {
            if (slottedStack.isValidInContainer(size)) {
                this.items.set(slottedStack.slot(), slottedStack.stack());
            }
        }
    }

    public interface ErrorableGetter<T> {
        T getEntry(Consumer<String> errorConsumer);
    }
}
