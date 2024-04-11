package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.slot.SlotAttribute;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.networking.client.SyncEntireContainer;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

public record AccessoriesCapabilityImpl(LivingEntity entity) implements AccessoriesCapability, InstanceCodecable {

    public AccessoriesCapabilityImpl(LivingEntity entity) {
        this.entity = entity;

        if (holder().loadedFromTag) this.clear();
    }

    @Override
    public AccessoriesHolder getHolder() {
        var holder = AccessoriesInternals.getHolder(entity);

        if (((AccessoriesHolderImpl) holder).loadedFromTag) this.clear();

        return holder;
    }

    private AccessoriesHolderImpl holder() {
        return (AccessoriesHolderImpl) this.getHolder();
    }

    @Override
    public Map<String, AccessoriesContainer> getContainers() {
        return this.holder().getSlotContainers();
    }

    @Override
    public void clear() {
        if (this.entity.level().isClientSide()) return;

        var holder = ((AccessoriesHolderImpl) AccessoriesInternals.getHolder(entity));

        var oldContainers = Map.copyOf(holder.getSlotContainers());

        holder.init(this);

        var currentContainers = holder.getSlotContainers();

        oldContainers.forEach((s, oldContainer) -> currentContainers.get(s).getAccessories().setFromPrev(oldContainer.getAccessories()));

        if (!(this.entity instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) return;

        var tag = new CompoundTag();

        holder.write(tag);

        AccessoriesInternals.getNetworkHandler().sendToTrackingAndSelf(this.entity(), new SyncEntireContainer(tag, this.entity.getId()));
    }

    @Override
    public void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.holder().getSlotContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if (!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(container::addModifier);
        }
    }

    @Override
    public void addPersistentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.holder().getSlotContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if (!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(container::addPersistentModifier);
        }
    }

    @Override
    public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.holder().getSlotContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if (!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(modifier -> container.removeModifier(modifier.getId()));
        }
    }

    @Override
    public Multimap<String, AttributeModifier> getSlotModifiers() {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();

        this.holder().getSlotContainers().forEach((s, container) -> modifiers.putAll(s, container.getModifiers().values()));

        return modifiers;
    }

    @Override
    public void clearSlotModifiers() {
        this.holder().getSlotContainers().forEach((s, container) -> container.clearModifiers());
    }

    @Override
    public void clearCachedSlotModifiers() {
        var slotModifiers = HashMultimap.<String, AttributeModifier>create();

        var containers = this.holder().getSlotContainers();

        containers.forEach((name, container) -> {
            var modifiers = container.getCachedModifiers();

            if (modifiers.isEmpty()) return;

            var accessories = container.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var stack = accessories.getItem(i);

                if (stack.isEmpty()) continue;

                var slotReference = new SlotReference(container.getSlotName(), this.entity, i);

                var map = AccessoriesAPI.getAttributeModifiers(stack, slotReference, AccessoriesAPI.getOrCreateSlotUUID(container.getSlotName(), i));

                for (Attribute attribute : map.keySet()) {
                    if (attribute instanceof SlotAttribute slotAttribute)
                        slotModifiers.putAll(slotAttribute.slotName(), map.get(slotAttribute));
                }
            }
        });

        slotModifiers.asMap().forEach((name, modifier) -> {
            if (!containers.containsKey(name)) return;

            var container = containers.get(name);

            modifier.forEach(container.getCachedModifiers()::remove);
            container.clearCachedModifiers();
        });
    }

    public Set<AccessoriesContainer> getUpdatingInventories() {
        return this.holder().containersRequiringUpdates;
    }

    @Override
    @Nullable
    public Pair<SlotReference, List<ItemStack>> equipAccessory(ItemStack stack, boolean allowSwapping, TriFunction<Accessory, ItemStack, SlotReference, Boolean> additionalCheck) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

        if (accessory == null) return null;

        var validContainers = new HashMap<String, AccessoriesContainer>();

        if (stack.isEmpty() && allowSwapping) {
            EntitySlotLoader.getEntitySlots(this.entity())
                    .forEach((s, slotType) -> {
                        validContainers.put(s, this.tryAndGetContainer(slotType));
                    });
        } else {
            // First attempt to equip an accessory within empty slot
            for (var continerEntry : this.getContainers().entrySet()) {
                var slotType = continerEntry.getKey();
                var container = continerEntry.getValue();

                if (container.getSize() <= 0) continue;

                var accessories = container.getAccessories();

                boolean isValid = AccessoriesAPI.canInsertIntoSlot(stack, new SlotReference(slotType, entity(), 0));

                if (!isValid) continue;

                if (allowSwapping) validContainers.put(slotType, container);

                for (int i = 0; i < container.getSize(); i++) {
                    var slotStack = accessories.getItem(i);
                    var slotReference = new SlotReference(slotType, entity(), i);

                    if (!slotStack.isEmpty()) continue;

                    if (!AccessoriesAPI.canUnequip(slotStack, slotReference)) continue;

                    if (additionalCheck.apply(accessory, stack, slotReference) && AccessoriesAPI.canInsertIntoSlot(stack, slotReference)) {
                        var stackCopy = stack.copy();

                        if (!entity.level().isClientSide) {
                            var splitStack = stackCopy.split(accessory.maxStackSize(stackCopy));

                            accessories.setItem(i, splitStack);

                            container.markChanged();
                        }

                        return Pair.of(
                                new SlotReference(container.getSlotName(), entity(), i),
                                List.of(stackCopy.isEmpty() ? ItemStack.EMPTY : stackCopy)
                        );
                    }
                }
            }
        }

        // Second attempt to equip an accessory within the first slot by swapping if allowed
        for (var validContainerEntry : validContainers.entrySet()) {
            var slotType = validContainerEntry.getKey();
            var validContainer = validContainerEntry.getValue();

            var accessories = validContainer.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var slotStack = accessories.getItem(i).copy();
                var slotReference = new SlotReference(slotType, entity(), i);

                if (!AccessoriesAPI.canUnequip(slotStack, slotReference) || slotStack.isEmpty()) continue;

                if (stack.isEmpty() || (additionalCheck.apply(accessory, stack, slotReference) && AccessoriesAPI.canInsertIntoSlot(stack, slotReference))) {
                    var stackCopy = stack.copy();

                    var splitStack = stackCopy.isEmpty() ? ItemStack.EMPTY : stackCopy.split(accessory.maxStackSize(stackCopy));

                    if (!entity.level().isClientSide) {
                        accessories.setItem(i, splitStack);

                        validContainer.markChanged();
                    }

                    return Pair.of(
                            new SlotReference(validContainer.getSlotName(), entity(), i),
                            List.of(stackCopy, slotStack)
                    );
                }
            }
        }

        return null;
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        return getFirstEquipped(predicate).isPresent();
    }

    public Optional<SlotEntryReference> getFirstEquipped(Predicate<ItemStack> predicate) {
        for (var container : this.holder().getSlotContainers().values()) {
            for (var stackEntry : container.getAccessories()) {
                var stack = stackEntry.getSecond();

                var reference = new SlotReference(container.getSlotName(), this.entity(), stackEntry.getFirst());

                var entryReference = recursiveStackHandling(stack, reference, (innerStack, ref) -> {
                    if (!innerStack.isEmpty() && predicate.test(stack)) {
                        return new SlotEntryReference(reference, stack);
                    }

                    return null;
                });

                if(entryReference != null) return Optional.of(entryReference);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate) {
        return getAllEquipped().stream().filter(reference -> predicate.test(reference.stack())).toList();
    }

    @Override
    public List<SlotEntryReference> getAllEquipped() {
        var references = new ArrayList<SlotEntryReference>();

        for (var container : this.holder().getSlotContainers().values()) {
            for (var stackEntry : container.getAccessories()) {
                var stack = stackEntry.getSecond();

                if (stack.isEmpty()) continue;

                var reference = new SlotReference(container.getSlotName(), this.entity(), stackEntry.getFirst());

                recursiveStackConsumption(stack, reference, (innerStack, ref) -> references.add(new SlotEntryReference(ref, innerStack)));
            }
        }

        return references;
    }

    private <T> @Nullable T recursiveStackHandling(ItemStack stack, SlotReference reference, BiFunction<ItemStack, SlotReference, @Nullable T> function) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

        var value = function.apply(stack, reference);

        if (accessory instanceof AccessoryNest holdable && value == null) {
            for (ItemStack innerStack : holdable.getInnerStacks(stack)) {
                if (innerStack.isEmpty()) continue;

                value = recursiveStackHandling(stack, reference, function);
            }
        }

        return value;
    }

    private void recursiveStackConsumption(ItemStack stack, SlotReference reference, BiConsumer<ItemStack, SlotReference> consumer) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

        consumer.accept(stack, reference);

        if (accessory instanceof AccessoryNest holdable) {
            for (ItemStack innerStack : holdable.getInnerStacks(stack)) {
                if (innerStack.isEmpty()) continue;

                recursiveStackConsumption(stack, reference, consumer);
            }
        }
    }

    @Override
    public void write(CompoundTag tag) {
        this.holder().write(tag);
    }

    @Override
    public void read(CompoundTag tag) {
        this.holder().read(tag);
    }
}
