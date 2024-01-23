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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AccessoriesCapabilityImpl implements AccessoriesCapability {

    private final LivingEntity entity;
    private final AccessoriesHolderImpl holder;

    public AccessoriesCapabilityImpl(LivingEntity entity){
        this.entity = entity;
        this.holder = (AccessoriesHolderImpl) AccessoriesAccess.getHolder(entity);
    }

    @Override
    public LivingEntity getEntity() {
        return this.entity;
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

                var slotReference = new SlotReference(container.slotType(), this.entity, i);

                var api = AccessoriesAccess.getAPI();

                var map = AccessoriesAPI.getAttributeModifiers(stack, slotReference, api.getOrCreateSlotUUID(container.slotType(), i));

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
    public boolean equipAccessory(ItemStack stack) {
        //TODO: IMPLEMENT
        return false;
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        for (var containerEntry : this.holder.getSlotContainers().entrySet()) {
            for (var stackEntry : containerEntry.getValue().getAccessories()) {
                if(predicate.test(stackEntry.getSecond())) return true;
            }
        }

        return false;
    }

    @Override
    public List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate) {
        var references = new ArrayList<SlotEntryReference>();

        for (var containerEntry : this.holder.getSlotContainers().entrySet()) {
            var container = containerEntry.getValue();

            for (var stackEntry : containerEntry.getValue().getAccessories()) {
                if(!predicate.test(stackEntry.getSecond())) continue;

                references.add(new SlotEntryReference(
                        new SlotReference(container.slotType(), container.capability().getEntity(), stackEntry.getFirst()),
                        stackEntry.getSecond()
                ));
            }
        }

        return references;
    }

    @Override
    public List<SlotEntryReference> getAllEquipped() {
        var references = new ArrayList<SlotEntryReference>();

        for (var containerEntry : this.holder.getSlotContainers().entrySet()) {
            var container = containerEntry.getValue();

            for (var stackEntry : containerEntry.getValue().getAccessories()) {
                references.add(new SlotEntryReference(
                        new SlotReference(container.slotType(), container.capability().getEntity(), stackEntry.getFirst()),
                        stackEntry.getSecond()
                ));
            }
        }

        return references;
    }

    @Override
    public void foreach(Consumer<SlotEntryReference> consumer) {
        for (var entry : this.holder.getSlotContainers().entrySet()) {
            var container = entry.getValue();

            var accessories = container.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var stack = accessories.getItem(i);

                consumer.accept(
                        new SlotEntryReference(
                                new SlotReference(
                                        container.slotType(),
                                        entity,
                                        i
                                ),
                                stack
                        )
                );
            }
        }
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
