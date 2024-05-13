package io.wispforest.accessories.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.slot.SlotReference;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import net.fabricmc.loader.api.FabricLoader;
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
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * An implementation of SimpleContainer with easy utilities for iterating over the stacks
 * and holding on to previous stack info
 */
public class ExpandedSimpleContainer extends SimpleContainer implements Iterable<Pair<Integer, ItemStack>> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String name;
    private final NonNullList<ItemStack> previousItems;
    private final Int2BooleanMap setFlags = new Int2BooleanArrayMap();

    private boolean newlyConstructed;

    private ExpandedSimpleContainer(int size) {
        this(size, "");
    }

    public ExpandedSimpleContainer(int size, String name) {
        this(size, name, true);
    }

    public ExpandedSimpleContainer(int size, String name, boolean toggleNewlyConstructed) {
        super(size);

        if(toggleNewlyConstructed) this.newlyConstructed = true;

        this.name = name;
        this.previousItems = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    private ExpandedSimpleContainer(ItemStack... items) {
        super(items);

        this.newlyConstructed = true;

        this.name = "";
        this.previousItems = NonNullList.withSize(items.length, ItemStack.EMPTY);
    }

    //--

    public boolean wasNewlyConstructed() {
        var bl = newlyConstructed;

        this.newlyConstructed = false;

        return bl;
    }

    public boolean isSlotFlaged(int slot){
        var bl = setFlags.getOrDefault(slot, false);

        if(bl) setFlags.put(slot, false);

        return bl;
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

    //--

    @Override
    public ItemStack getItem(int slot) {
        if(!validIndex(slot)) return ItemStack.EMPTY;

        return super.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if(!validIndex(slot)) return ItemStack.EMPTY;

        var stack = super.removeItem(slot, amount);

        if (!stack.isEmpty()) {
            setFlags.put(slot, true);
        }

        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if(!validIndex(slot)) return ItemStack.EMPTY;

        // TODO: Concerning the flagging system, should such work for it?

        return super.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if(!validIndex(slot)) return;

        super.setItem(slot, stack);

        setFlags.put(slot, true);
    }

    // Simple validation method to make sure that the given access is valid before attempting an operation
    public boolean validIndex(int slot){
        var isValid = slot >= 0 && slot < this.getContainerSize();

        var nameInfo = (this.name != null ? "Container: " + this.name + ", " : "");

        if(!isValid && FabricLoader.getInstance().isDevelopmentEnvironment()){
            try {
                throw new IllegalStateException("Access to a given Inventory was found to be out of the range valid for the container! [Name: " + nameInfo + " Index: " + slot + "]");
            } catch (Exception e) {
                LOGGER.debug("Full Exception: ", e);
            }
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
            var compoundTag = containerNbt.getCompound(i);

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
                var compoundTag = new CompoundTag();

                compoundTag.putInt("Slot", i);

                listTag.add(itemStack.save(compoundTag));
            }
        }

        return listTag;
    }

    //--

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

    public void setFromPrev(ExpandedSimpleContainer prevContainer) {
        prevContainer.forEach(pair -> this.setPreviousItem(pair.getFirst(), pair.getSecond()));
    }

    public void copyPrev(ExpandedSimpleContainer prevContainer) {
        for (int i = 0; i < prevContainer.getContainerSize(); i++) {
            if(i >= this.getContainerSize()) continue;

            var prevItem = prevContainer.getPreviousItem(i);

            if(!prevItem.isEmpty()) this.setPreviousItem(i, prevItem);
        }
    }
}

