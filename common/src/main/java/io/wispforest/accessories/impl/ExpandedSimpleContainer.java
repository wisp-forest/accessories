package io.wispforest.accessories.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.function.BiConsumer;

public class ExpandedSimpleContainer extends SimpleContainer implements Iterable<Pair<Integer, ItemStack>> {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private String name = null;
    private final NonNullList<ItemStack> previousItems;

    public ExpandedSimpleContainer(int size) {
        this(size, "");
    }

    public ExpandedSimpleContainer(int size, String name) {
        super(size);

        this.name = name;
        this.previousItems = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public ExpandedSimpleContainer(ItemStack... items) {
        super(items);

        this.previousItems = NonNullList.withSize(items.length, ItemStack.EMPTY);
    }

    public void setPreviousItem(int slot, ItemStack stack) {
        this.previousItems.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        this.setChanged();
    }

    public ItemStack getPreviousItem(int slot) {
        return slot >= 0 && slot < this.previousItems.size()
                ? this.previousItems.get(slot)
                : ItemStack.EMPTY;
    }

    public void iterateInventory(BiConsumer<Integer, ItemStack> consumer){
        for (int i = 0; i < this.getContainerSize(); i++) {
            consumer.accept(i, this.getItem(i));
        }
    }

    //--

    @Override
    public ItemStack getItem(int slot) {
        if(!validIndex(slot)) return ItemStack.EMPTY;

        return super.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if(!validIndex(slot)) return ItemStack.EMPTY;

        return super.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if(!validIndex(slot)) return ItemStack.EMPTY;

        return super.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if(!validIndex(slot)) return;

        super.setItem(slot, stack);
    }

    private boolean validIndex(int slot){
        var isValid = slot >= 0 && slot < this.getContainerSize();

        var nameInfo = (this.name != null ? "Container: " + this.name + ", " : "");

        if(!isValid){
            LOGGER.error("Access to a given Inventory was found to be out of the range valid for the container! [{}Index: {}]", nameInfo, slot);
        }

        return isValid;
    }

    //--

    @Override
    public void fromTag(ListTag containerNbt) {
        for(int i = 0; i < this.getContainerSize(); ++i) {
            this.setItem(i, ItemStack.EMPTY);
        }

        for(int i = 0; i < containerNbt.size(); ++i) {
            CompoundTag compoundTag = containerNbt.getCompound(i);
            int j = compoundTag.getInt("Slot");
            if (j >= 0 && j < this.getContainerSize()) {
                this.setItem(j, ItemStack.of(compoundTag));
            }
        }
    }

    @Override
    public ListTag createTag() {
        ListTag listTag = new ListTag();

        for(int i = 0; i < this.getContainerSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (!itemStack.isEmpty()) {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putInt("Slot", i);
                itemStack.save(compoundTag);
                listTag.add(compoundTag);
            }
        }

        return listTag;
    }

    @NotNull
    @Override
    public Iterator<Pair<Integer, ItemStack>> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < ExpandedSimpleContainer.this.getContainerSize();
            }

            @Override
            public Pair<Integer, ItemStack> next() {
                var pair = new Pair<>(index, ExpandedSimpleContainer.this.getItem(index));

                index++;

                return pair;
            }
        };
    }
}

