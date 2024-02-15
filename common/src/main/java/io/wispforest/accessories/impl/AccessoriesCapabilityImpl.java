package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.api.SlotAttribute;
import io.wispforest.accessories.api.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class AccessoriesCapabilityImpl implements AccessoriesCapability {

    private final LivingEntity entity;
    private final AccessoriesHolderImpl holder;

    public AccessoriesCapabilityImpl(LivingEntity entity){
        this.entity = entity;
        this.holder = (AccessoriesHolderImpl) AccessoriesAccess.getHolder(entity);

        if(this.holder.loadedFromTag) this.clear();
    }

    @Override
    public LivingEntity getEntity() {
        return this.entity;
    }

    @Override
    public AccessoriesHolder getHolder() {
        return this.holder;
    }

    @Override
    public Map<String, AccessoriesContainer> getContainers() {
        return this.holder.getSlotContainers();
    }

    public void addInvalidStacks(Collection<ItemStack> stacks){
        this.holder.invalidStacks.addAll(stacks);
    }

    @Override
    public void clear() {
        this.holder.init(this);
    }

    @Override
    public void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.holder.getSlotContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if(!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(container::addModifier);
        }
    }

    @Override
    public void addPersistentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.holder.getSlotContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if(!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(container::addPersistentModifier);
        }
    }

    @Override
    public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.holder.getSlotContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if(!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(modifier -> container.removeModifier(modifier.getId()));
        }
    }

    @Override
    public Multimap<String, AttributeModifier> getSlotModifiers() {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();

        this.holder.getSlotContainers().forEach((s, container) -> modifiers.putAll(s, container.getModifiers().values()));

        return modifiers;
    }

    @Override
    public void clearSlotModifiers() {
        this.holder.getSlotContainers().forEach((s, container) -> container.clearModifiers());
    }

    @Override
    public void clearCachedSlotModifiers() {
        Multimap<String, AttributeModifier> slotModifiers = HashMultimap.create();

        var containers = this.holder.getSlotContainers();

        containers.forEach((name, container) -> {
            var modifiers = container.getCachedModifiers();

            if(modifiers.isEmpty()) return;

            var accessories = container.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var stack = accessories.getItem(i);

                if(stack.isEmpty()) continue;

                var slotReference = new SlotReference(container.getSlotName(), this.entity, i);

                var map = AccessoriesAPI.getAttributeModifiers(stack, slotReference, AccessoriesAPI.getOrCreateSlotUUID(container.getSlotName(), i));

                for (Attribute attribute : map.keySet()) {
                    if(attribute instanceof SlotAttribute slotAttribute) slotModifiers.putAll(slotAttribute.slotName(), map.get(slotAttribute));
                }
            }
        });

        slotModifiers.asMap().forEach((name, modifier) -> {
            if(!containers.containsKey(name)) return;

            var container = containers.get(name);

            modifier.forEach(container.getCachedModifiers()::remove);
            container.clearCachedModifiers();
        });
    }

    public Set<AccessoriesContainer> getUpdatingInventories(){
        return this.holder.containersRequiringUpdates;
    }

    @Override
    @Nullable
    public SlotEntryReference equipAccessory(ItemStack stack, boolean allowSwapping, TriFunction<Accessory, ItemStack, SlotReference, Boolean> additionalCheck) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

        if(accessory == null) return null;

        var validContainers = new HashMap<String, AccessoriesContainer>();

        // First attempt to equip an accessory within empty slot
        for (var continerEntry : this.getContainers().entrySet()) {
            var slotType = continerEntry.getKey();
            var container = continerEntry.getValue();

            var accessories = container.getAccessories();

            boolean isValid = AccessoriesAPI.canInsertIntoSlot(stack, new SlotReference(slotType, getEntity(), 0));

            if(!isValid || container.getSize() <= 0) continue;

            if(allowSwapping) validContainers.put(slotType, container);

            for (int i = 0; i < container.getSize(); i++) {
                var slotStack = accessories.getItem(i);
                var slotReference = new SlotReference(slotType, getEntity(), i);

                if(!slotStack.isEmpty()) continue;

                if(!AccessoriesAPI.canUnequip(slotStack, slotReference)) continue;

                if(additionalCheck.apply(accessory, stack, slotReference) && AccessoriesAPI.canInsertIntoSlot(stack, slotReference)) {
                    if (!entity.level().isClientSide) {
                        accessories.setItem(i, stack);

                        container.markChanged();
                    }

                    return new SlotEntryReference(
                            new SlotReference(container.getSlotName(), getEntity(), i),
                            ItemStack.EMPTY
                    );
                }
            }
        }

        // Second attempt to equip an accessory within the first slot by swapping if allowed
        for (var validContainerEntry : validContainers.entrySet()) {
            var slotType = validContainerEntry.getKey();
            var validContainer = validContainerEntry.getValue();

            var accessories = validContainer.getAccessories();

            var slotStack = accessories.getItem(0);
            var slotReference = new SlotReference(slotType, getEntity(), 0);

            if(!AccessoriesAPI.canUnequip(slotStack, slotReference)) continue;

            if (additionalCheck.apply(accessory, stack, slotReference) && AccessoriesAPI.canInsertIntoSlot(stack, slotReference)) {
                if (!entity.level().isClientSide) {
                    accessories.setItem(0, stack);

                    validContainer.markChanged();
                }

                return new SlotEntryReference(new SlotReference(validContainer.getSlotName(), getEntity(), 0), slotStack);
            }
        }

        return null;
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        for (var containerEntry : this.holder.getSlotContainers().entrySet()) {
            for (var stackEntry : containerEntry.getValue().getAccessories()) {
                var stack = stackEntry.getSecond();

                var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

                if(predicate.test(stack)) return true;

                if(accessory instanceof AccessoryNest holdable){
                    for (ItemStack innerStack : holdable.getInnerStacks(stackEntry.getSecond())) {
                        if(predicate.test(innerStack)) return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate) {
        return getAllEquipped().stream().filter(reference -> predicate.test(reference.stack())).toList();
    }

    @Override
    public List<SlotEntryReference> getAllEquipped() {
        var references = new ArrayList<SlotEntryReference>();

        for (var containerEntry : this.holder.getSlotContainers().entrySet()) {
            var container = containerEntry.getValue();

            for (var stackEntry : containerEntry.getValue().getAccessories()) {
                var stack = stackEntry.getSecond();
                var reference = new SlotReference(container.getSlotName(), container.capability().getEntity(), stackEntry.getFirst());

                var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

                references.add(new SlotEntryReference(reference, stack));

                if(accessory instanceof AccessoryNest holdable){
                    for (ItemStack innerStack : holdable.getInnerStacks(stackEntry.getSecond())) {
                        references.add(new SlotEntryReference(reference, innerStack));
                    }
                }
            }
        }

        return references;
    }

    @Override
    public void write(CompoundTag tag) {
        this.holder.write(tag);
    }

    @Override
    public void read(CompoundTag tag) {
        this.holder.read(tag);
    }
}
