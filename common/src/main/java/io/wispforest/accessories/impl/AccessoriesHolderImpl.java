package io.wispforest.accessories.impl;

import io.wispforest.accessories.api.*;
import io.wispforest.accessories.data.EntitySlotLoader;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

@ApiStatus.Internal
public class AccessoriesHolderImpl implements AccessoriesHolder, InstanceCodecable {

    private final Map<String, AccessoriesContainer> slotContainers = new LinkedHashMap<>();

    public final List<ItemStack> invalidStacks = new ArrayList<>();
    protected final Set<AccessoriesContainer> containersRequiringUpdates = new HashSet<>();

    private boolean showUnusedSlots = false;
    private boolean cosmeticsShown = false;

    private int scrolledSlot = 0;

    private boolean linesShown = false;

    private CompoundTag tag;
    protected boolean loadedFromTag = false;

    public static AccessoriesHolderImpl of(){
        var holder = new AccessoriesHolderImpl();

        holder.loadedFromTag = true;
        holder.tag = new CompoundTag();

        return holder;
    }

    @ApiStatus.Internal
    protected Map<String, AccessoriesContainer> getSlotContainers() {
        return this.slotContainers;
    }

    @Override
    public boolean cosmeticsShown() {
        return this.cosmeticsShown;
    }

    @Override
    public AccessoriesHolder cosmeticsShown(boolean value) {
        this.cosmeticsShown = value;

        return this;
    }

    @Override
    public int scrolledSlot() {
        return this.scrolledSlot;
    }

    @Override
    public AccessoriesHolder scrolledSlot(int slot) {
        this.scrolledSlot = slot;

        return this;
    }

    @Override
    public boolean linesShown() {
        return this.linesShown;
    }

    @Override
    public AccessoriesHolder linesShown(boolean value) {
        this.linesShown = value;

        return this;
    }

    @Override
    public boolean showUnusedSlots() {
        return this.showUnusedSlots;
    }

    @Override
    public AccessoriesHolder showUnusedSlots(boolean value) {
        this.showUnusedSlots = value;

        return this;
    }

    public void init(AccessoriesCapability capability) {
        var livingEntity = capability.entity();

        this.slotContainers.clear();
        //this.invalidStacks.clear();

        if (loadedFromTag) {
            EntitySlotLoader.getEntitySlots(livingEntity).forEach((s, slotType) -> {
                slotContainers.putIfAbsent(s, new AccessoriesContainerImpl(capability, slotType));
            });

            read(capability, livingEntity, this.tag);
        } else {
            EntitySlotLoader.getEntitySlots(livingEntity).forEach((s, slotType) -> {
                slotContainers.put(s, new AccessoriesContainerImpl(capability, slotType));
            });
        }
    }

    public static final String CONTAINERS_KEY = "AccessoriesContainers";

    public static final String COSMETICS_SHOWN_KEY = "CosmeticsShown";

    public static final String LINES_SHOWN_KEY = "LinesShown";

    @Override
    public void write(CompoundTag tag) {
        if(slotContainers.size() == 0) {
            return;
        }

        tag.putBoolean(COSMETICS_SHOWN_KEY, cosmeticsShown);

        tag.putBoolean(LINES_SHOWN_KEY, linesShown);

        //--

        CompoundTag main = new CompoundTag();

        this.slotContainers.forEach((s, container) -> {
            main.put(s, Util.make(new CompoundTag(), innerTag -> ((AccessoriesContainerImpl) container).write(innerTag)));
        });

        tag.put(CONTAINERS_KEY, main);
    }

    public void read(LivingEntity entity, CompoundTag tag) {
        read(entity.accessoriesCapability(), entity, tag);
    }

    public void read(AccessoriesCapability capability, LivingEntity entity, CompoundTag tag) {
        this.loadedFromTag = false;

        var slots = EntitySlotLoader.getEntitySlots(entity);

        this.cosmeticsShown = tag.getBoolean(COSMETICS_SHOWN_KEY);

        this.linesShown = tag.getBoolean(LINES_SHOWN_KEY);

        var containersTag = tag.contains(CONTAINERS_KEY) ? tag.getCompound(CONTAINERS_KEY) : tag.getCompound("Accessories");

        for (String key : containersTag.getAllKeys()) {
            var containerTag = containersTag.getCompound(key);

            if (slots.containsKey(key)) {
                var container = slotContainers.get(key);

                var prevAccessories = AccessoriesContainerImpl.copyContainerList(container.getAccessories());
                var prevCosmetics = AccessoriesContainerImpl.copyContainerList(container.getCosmeticAccessories());

                ((AccessoriesContainerImpl) container).read(containerTag);

                if (prevAccessories.getContainerSize() > container.getSize()) {
                    for (int i = container.getSize() - 1; i < prevAccessories.getContainerSize(); i++) {
                        var prevStack = prevAccessories.getItem(i);

                        if (!prevStack.isEmpty()) {
                            this.invalidStacks.add(prevStack);
                        }

                        var prevCosmetic = prevCosmetics.getItem(i);

                        if (!prevCosmetic.isEmpty()) {
                            this.invalidStacks.add(prevCosmetic);
                        }
                    }
                }
            } else {
                var containers = AccessoriesContainerImpl.readContainers(containerTag, AccessoriesContainerImpl.COSMETICS_KEY, AccessoriesContainerImpl.ITEMS_KEY);

                for (SimpleContainer simpleContainer : containers) {
                    for (int i = 0; i < simpleContainer.getContainerSize(); i++) {
                        var stack = simpleContainer.getItem(i);

                        if (!stack.isEmpty()) this.invalidStacks.add(stack);
                    }
                }
            }
        }

        capability.clearCachedSlotModifiers();

        this.tag = new CompoundTag();
    }

    @Override
    public void read(CompoundTag tag) {
        this.loadedFromTag = true;
        this.tag = tag;
    }
}