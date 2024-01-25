package io.wispforest.accessories.impl;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.SlotType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class AccessoriesHolderImpl implements AccessoriesHolder {

    private final Map<String, AccessoriesContainer> slotContainers = new HashMap<>();

    protected final List<ItemStack> invalidStacks = new ArrayList<>();
    protected final Set<AccessoriesContainer> containersRequiringUpdates = new HashSet<>();

    private CompoundTag tag;
    protected boolean loadedFromTag = false;

    public void init(AccessoriesCapability capability){
        var livingEntity = capability.getEntity();

        this.slotContainers.clear();
        this.invalidStacks.clear();

        var api = AccessoriesAccess.getAPI();

        if(loadedFromTag) {
            api.getEntitySlots(livingEntity).forEach((s, slotType) -> {
                slotContainers.putIfAbsent(s, new AccessoriesContainerImpl(capability, slotType));
            });

            read(livingEntity, this.tag);

            return;
        } else {
            api.getEntitySlots(livingEntity).forEach((s, slotType) -> {
                slotContainers.put(s, new AccessoriesContainerImpl(capability, slotType));
            });
        }
    }

    @Override
    public Map<String, AccessoriesContainer> getSlotContainers() {
        return slotContainers;
    }

    public static final String MAIN_KEY = "Accessories";

    @Override
    public void write(CompoundTag tag) {
        CompoundTag main = new CompoundTag();

        for (var entry : this.slotContainers.entrySet()) {
            var containerTag = new CompoundTag();

            entry.getValue().write(containerTag);

            main.put(entry.getKey(), containerTag);
        }

        tag.put(MAIN_KEY, main);
    }

    public void read(LivingEntity entity, CompoundTag tag) {
        var slots = AccessoriesAccess.getAPI().getEntitySlots(entity);

        var containersTag = tag.getCompound(MAIN_KEY);

        for (String key : containersTag.getAllKeys()) {
            var containerTag = containersTag.getCompound(key);

            if(slots.containsKey(key)){
                var container = slotContainers.get(key);

                var prevAccessories = AccessoriesContainerImpl.copyContainerList(container.getAccessories());
                var prevCosmetics = AccessoriesContainerImpl.copyContainerList(container.getCosmeticAccessories());

                container.read(containerTag);

                if(prevAccessories.getContainerSize() > container.getSize()){
                    for (int i = container.getSize() - 1; i < prevAccessories.getContainerSize(); i++) {
                        var prevStack = prevAccessories.getItem(i);

                        if(!prevStack.isEmpty()){
                            this.invalidStacks.add(prevStack);
                        }

                        var prevCosmetic = prevCosmetics.getItem(i);

                        if(!prevCosmetic.isEmpty()){
                            this.invalidStacks.add(prevCosmetic);
                        }
                    }
                }
            } else {
                var containers = AccessoriesContainerImpl.readContainers(containerTag, AccessoriesContainerImpl.COSMETICS_KEY, AccessoriesContainerImpl.ITEMS_KEY);

                for (SimpleContainer simpleContainer : containers) {
                    for (int i = 0; i < simpleContainer.getContainerSize(); i++) {
                        var stack = simpleContainer.getItem(i);

                        if(!stack.isEmpty()) this.invalidStacks.add(stack);
                    }
                }
            }
        }

        this.loadedFromTag = false;
        this.tag = new CompoundTag();
    }

    @Override
    public void read(CompoundTag tag) {
        this.loadedFromTag = true;
        this.tag = tag;
    }


}
