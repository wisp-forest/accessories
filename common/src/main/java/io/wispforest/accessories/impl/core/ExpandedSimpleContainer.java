package io.wispforest.accessories.impl.core;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.utils.BaseContainer;
import io.wispforest.accessories.utils.ImmutableContainer;
import io.wispforest.accessories.utils.ItemStackMutation;
import io.wispforest.accessories.utils.ItemStackResize;
import io.wispforest.owo.util.EventSource;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * An implementation of SimpleContainer with easy utilities for iterating over the stacks
 * and holding on to previous stack info
 */
public class ExpandedSimpleContainer extends BaseContainer implements Iterable<ItemStack> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final AccessoriesContainerImpl container;

    private final String name;

    private final NonNullList<ItemStack> previousItems;

    private final Int2BooleanMap setFlags = new Int2BooleanArrayMap();

    private boolean canFlagSetCalls = true;

    private boolean newlyConstructed;

    private final Int2ObjectMap<EventSource.Subscription> currentMutationSubscriptions = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<EventSource.Subscription> currentResizeSubscriptions = new Int2ObjectOpenHashMap<>();

    private final Int2ObjectMap<EventSource.Subscription> currentPrevResizeSubscriptions = new Int2ObjectOpenHashMap<>();

    public ExpandedSimpleContainer(AccessoriesContainerImpl container, int size, String name) {
        this(container, size, name, true);
    }

    public ExpandedSimpleContainer(AccessoriesContainerImpl container, int size, String name, boolean toggleNewlyConstructed) {
        super(size);

        this.container = container;

        this.addListener(container);

        if(toggleNewlyConstructed) this.newlyConstructed = true;

        this.name = name;

        this.previousItems = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public String name() {
        return this.name;
    }

    public Container toImmutable() {
        return new ImmutableContainer(this.getItems());
    }

    //--

    public boolean wasNewlyConstructed() {
        var bl = newlyConstructed;

        this.newlyConstructed = false;

        return bl;
    }

    public boolean isSlotFlagged(int slot){
        var bl = setFlags.getOrDefault(slot, false);

        if(bl) setFlags.put(slot, false);

        return bl;
    }

    void toggleFlagablity() {
        canFlagSetCalls = !canFlagSetCalls;
    }

    private void removeAllSlotSubscription(int slot) {
        removeMutationSubscription(slot);
        removeResizeSubscription(slot);

        removePrevResizeSubscription(slot);
    }

    private void removePrevResizeSubscription(int slot) {
        var subscription = currentPrevResizeSubscriptions.remove(slot);

        if (subscription != null) subscription.cancel();
    }

    private void removeResizeSubscription(int slot) {
        var subscription = currentResizeSubscriptions.remove(slot);

        if (subscription != null) subscription.cancel();
    }

    private void removeMutationSubscription(int slot) {
        var subscription = currentMutationSubscriptions.remove(slot);

        if (subscription != null) subscription.cancel();
    }

    public void setPreviousItem(int slot, ItemStack stack) {
        if(slot >= 0 && slot < this.previousItems.size()) {
            this.previousItems.set(slot, stack);

            removePrevResizeSubscription(slot);

            if (!stack.isEmpty()) {
                /*
                    TODO: MAY NEED TO DEAL WITH THIS BETTER I.E. CALLING UNEQUIP BEFORE OR SOMETHING BUT IDK
                    LIKE THIS ISSUE IS DOWN TO THE FACT THAT THE COUNT CAN BE ADJUSTED WITHOUT NOTIFYING THE CONTAINER OF THE CHANGE
                    MEANING THE REFERENCE WILL BE EMPTY LEADING TO NO UNEQUIP CALL BUT IT MEANS THE STACK DOSE
                    NOT HAVE THE CORRECT STACK IF IT WAS TRANSFERRED TO ANOTHER STACK
                 */
                var stackCopy = stack.copy();

                this.currentPrevResizeSubscriptions.put(slot, ItemStackResize.getEvent(stack).source().subscribe((stack1, prevSize) -> {
                    var isEmpty = stack1.getCount() <= 0;

                    if (isEmpty) {
                        this.previousItems.set(slot, stackCopy);

                        removePrevResizeSubscription(slot);
                    }
                }));
            }
        }
    }

    public ItemStack getPreviousItem(int slot) {
        return slot >= 0 && slot < this.previousItems.size()
                ? this.previousItems.get(slot)
                : ItemStack.EMPTY;
    }

    //--

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(itemStack);

        return Math.min(super.getMaxStackSize(itemStack), accessory.maxStackSize(itemStack));
    }

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
            if (canFlagSetCalls) setFlags.put(slot, true);

            var prevStack = this.getItem(slot);

            if (prevStack.isEmpty()) {
                removeMutationSubscription(slot);
                removeResizeSubscription(slot);
            }

            this.setPreviousItem(slot, stack);
        }

        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if(!validIndex(slot)) return ItemStack.EMPTY;

        // TODO: Concerning the flagging system, should this work for it?

        var stack = super.removeItemNoUpdate(slot);

        removeMutationSubscription(slot);
        removeResizeSubscription(slot);

        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if(!validIndex(slot)) return;

        removeMutationSubscription(slot);
        removeResizeSubscription(slot);

        super.setItem(slot, stack);

        if (!stack.isEmpty()) {
            this.currentMutationSubscriptions.put(slot,
                    ItemStackMutation.getEvent(stack).source().subscribe((stack1, types) -> {
                        if (types.contains(AccessoriesDataComponents.ATTRIBUTES) || types.contains(AccessoriesDataComponents.NESTED_ACCESSORIES)) {
                            this.setChanged();
                        }

                        if (!this.container.capability().entity().level().isClientSide()) {
                            var cache = AccessoriesHolderImpl.getHolder(this.container.capability()).getLookupCache();

                            if (cache != null) cache.invalidateLookupData(this.container.getSlotName(), stack1, types);
                        }
                    })
            );

            this.currentResizeSubscriptions.put(slot,
                    ItemStackResize.getEvent(stack).source().subscribe((stack1, prevSize) -> {
                        if (stack1.isEmpty()) {
                            this.setItem(slot, ItemStack.EMPTY);
                        }
                    })
            );
        }

        if (canFlagSetCalls) setFlags.put(slot, true);
    }

    // Simple validation method to make sure that the given access is valid before attempting an operation
    public boolean validIndex(int slot){
        var isValid = slot >= 0 && slot < this.getContainerSize();

        if(!isValid && FabricLoader.getInstance().isDevelopmentEnvironment()){
            var nameInfo = (this.name != null ? "Container: " + this.name + ", " : "");

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
    public void loadItemsFromList(Collection<ItemStackWithSlot> slottedStacks) {
        this.container.containerListenerLock = true;

        var capability = this.container.capability();

        var prevStacks = new ArrayList<ItemStack>();
        for(int i = 0; i < this.getContainerSize(); ++i) {
            var currentStack = this.getItem(i);

            prevStacks.add(currentStack);

            this.setItem(i, ItemStack.EMPTY);
        }

        var invalidStacks = new ArrayList<ItemStack>();
        var decodedStacks = new ArrayList<ItemStack>();

        for (var slottedStack : slottedStacks) {
            var stack = slottedStack.stack();

            decodedStacks.add(stack);

            if (slottedStack.isValidInContainer(this.getContainerSize())) {
                this.setItem(slottedStack.slot(), stack);
            } else {
                invalidStacks.add(stack);
            }
        }

        this.container.containerListenerLock = false;

        if (!capability.entity().level().isClientSide()) {
            if (!prevStacks.equals(decodedStacks)) {
                this.setChanged();
            }

            AccessoriesHolderImpl.getHolder(capability).invalidStacks.addAll(invalidStacks);
        }
    }

    //--

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < ExpandedSimpleContainer.this.getContainerSize();
            }

            @Override
            public ItemStack next() {
                var stack = ExpandedSimpleContainer.this.getItem(index);

                index++;

                return stack;
            }
        };
    }

    public void foreach(BiConsumer<Integer, ItemStack> consumer) {
        var i = 0;

        for (ItemStack itemStack : this) {
            consumer.accept(i, itemStack);
            i++;
        }
    }

    public <T> T foreach(BiFunction<Integer, ItemStack, @Nullable T> consumer) {
        var i = 0;

        for (ItemStack itemStack : this) {
            var result = consumer.apply(i, itemStack);

            if (result != null) return null;

            i++;
        }

        return null;
    }

    public void setFromPrev(ExpandedSimpleContainer prevContainer) {
        int i = 0;

        for (var itemStack : prevContainer) {
            prevContainer.removeMutationSubscription(i);
            this.setPreviousItem(i, itemStack);
            i++;
        }
    }

    public void copyPrev(ExpandedSimpleContainer prevContainer) {
        for (int i = 0; i < prevContainer.getContainerSize(); i++) {
            if(i >= this.getContainerSize()) continue;

            var prevItem = prevContainer.getPreviousItem(i);

            prevContainer.removeAllSlotSubscription(i);

            if(!prevItem.isEmpty()) this.setPreviousItem(i, prevItem);
        }
    }
}
