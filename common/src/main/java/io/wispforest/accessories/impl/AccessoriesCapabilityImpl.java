package io.wispforest.accessories.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.ExtraSlotTypeProperties;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.SyncEntireContainer;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.util.MapCarrier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

@ApiStatus.Internal
public class AccessoriesCapabilityImpl implements AccessoriesCapability, InstanceEndec {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final LivingEntity entity;

    public AccessoriesCapabilityImpl(LivingEntity entity) {
        this.entity = entity;

        // Runs various Init calls to properly setup holder
        getHolder();
    }

    @Override
    public LivingEntity entity() {
        return entity;
    }

    @Override
    public AccessoriesHolder getHolder() {
        var holder = ((AccessoriesHolderImpl) AccessoriesInternals.getHolder(entity));

        // Attempts to reset the container when loaded from tag on the server
        if (holder.loadedFromTag) this.reset(true);

        // Prevents containers from not existing even if a given entity will have such slots but have yet to be synced to the client
        if (holder.getSlotContainers().size() != EntitySlotLoader.getEntitySlots(entity).size()) holder.init(this);

        return holder;
    }

    private AccessoriesHolderImpl holder() {
        return (AccessoriesHolderImpl) this.getHolder();
    }

    @Override
    public Map<String, AccessoriesContainer> getContainers() {
        // Dirty patch to handle capability mismatch on containers when transferring it
        // TODO: Wonder if this is the best solution to the problem of desynced when data is copied
        for (var container : this.holder().getAllSlotContainers().values()) {
            if(this.entity == container.capability().entity()) break;

            ((AccessoriesContainerImpl) container).capability = this;
        }

        return this.holder().getSlotContainers();
    }

    @Override
    public void reset(boolean loadedFromTag) {
        if (this.entity.level().isClientSide()) return;

        var holder = ((AccessoriesHolderImpl) AccessoriesInternals.getHolder(entity));

        if (!loadedFromTag) {
            var oldContainers = Map.copyOf(holder.getSlotContainers());

            holder.init(this);

            var currentContainers = holder.getSlotContainers();

            oldContainers.forEach((s, oldContainer) -> {
                var currentContainer = currentContainers.get(s);

                currentContainer.getAccessories().setFromPrev(oldContainer.getAccessories());

                currentContainer.markChanged(false);
            });
        } else {
            holder.init(this);
        }

        if (!(this.entity instanceof ServerPlayer serverPlayer) || serverPlayer.connection == null) return;

        var carrier = NbtMapCarrier.of();

        holder.write(carrier, SerializationContext.attributes(RegistriesAttribute.of(this.entity.level().registryAccess())));

        AccessoriesNetworking.sendToTrackingAndSelf(serverPlayer, new SyncEntireContainer(serverPlayer.getId(), carrier));
    }

    private boolean updateContainersLock = false;

    @Override
    public void updateContainers() {
        if (updateContainersLock) return;

        boolean hasUpdateOccurred;

        var containers = this.getContainers().values();

        this.updateContainersLock = true;

        do {
            hasUpdateOccurred = false;

            for (var container : containers) {
                if (!container.hasChanged()) {
                    continue;
                }

                container.update();

                hasUpdateOccurred = true;
            }
        } while (hasUpdateOccurred);

        this.updateContainersLock = false;
    }

    @Override
    public void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.getContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if (!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(container::addTransientModifier);
        }
    }

    @Override
    public void addPersistentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.getContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if (!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(container::addPersistentModifier);
        }
    }

    @Override
    public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        var containers = this.getContainers();

        for (var entry : modifiers.asMap().entrySet()) {
            if (!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            entry.getValue().forEach(modifier -> container.removeModifier(modifier.id()));
        }
    }

    @Override
    public Multimap<String, AttributeModifier> getSlotModifiers() {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();

        this.getContainers().forEach((s, container) -> modifiers.putAll(s, container.getModifiers().values()));

        return modifiers;
    }

    @Override
    public void clearSlotModifiers() {
        this.getContainers().forEach((s, container) -> container.clearModifiers());
    }

    @Override
    public void clearCachedSlotModifiers() {
        var slotModifiers = HashMultimap.<String, AttributeModifier>create();

        var containers = this.getContainers();

        containers.forEach((name, container) -> {
            var modifiers = container.getCachedModifiers();

            if (modifiers.isEmpty()) return;

            var accessories = container.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var stack = accessories.getItem(i);

                if (stack.isEmpty()) continue;

                var slotReference = container.createReference(i);

                slotModifiers.putAll(AccessoriesAPI.getAttributeModifiers(stack, slotReference).getSlotModifiers());
            }
        });

        slotModifiers.asMap().forEach((name, modifiers) -> {
            if (!containers.containsKey(name)) return;

            var container = containers.get(name);

            modifiers.forEach(container.getCachedModifiers()::remove);

            container.clearCachedModifiers();
        });
    }

    public Map<AccessoriesContainer, Boolean> getUpdatingInventories() {
        return this.holder().containersRequiringUpdates;
    }

    //--

    @Nullable
    public Pair<SlotReference, EquipAction> canEquipAccessory(ItemStack stack, boolean allowSwapping, EquipCheck extraCheck) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

        if (accessory == null) return null;

        var validContainers = new HashMap<String, AccessoriesContainer>();

        if (stack.isEmpty() && allowSwapping) {
            var allContainers = this.getContainers();

            EntitySlotLoader.getEntitySlots(this.entity())
                    .forEach((s, slotType) -> validContainers.put(s, allContainers.get(slotType.name())));
        } else {
            // First attempt to equip an accessory within empty slot
            for (var container : this.getContainers().values()) {
                if (container.getSize() <= 0) continue;

                boolean isValid = AccessoriesAPI.canInsertIntoSlot(stack, container.createReference(0));

                // Prevents checking containers that will never allow for the given stack to be equipped within it
                if (!isValid || !ExtraSlotTypeProperties.getProperty(container.getSlotName(), entity.level().isClientSide).allowEquipFromUse()) continue;

                if (allowSwapping) validContainers.put(container.getSlotName(), container);

                var accessories = container.getAccessories();

                for (int i = 0; i < container.getSize(); i++) {
                    var slotStack = accessories.getItem(i);
                    var slotReference = container.createReference(i);

                    if (slotStack.isEmpty()
                            && AccessoriesAPI.canUnequip(slotStack, slotReference)
                            && AccessoriesAPI.canInsertIntoSlot(stack, slotReference)
                            && extraCheck.isValid(slotStack, false)) {
                        return Pair.of(container.createReference(i), (newStack) -> setStack(slotReference, newStack, false));
                    }
                }
            }
        }

        // Second attempt to equip an accessory within the first slot by swapping if allowed
        for (var validContainer : validContainers.values()) {
            var accessories = validContainer.getAccessories();

            for (int i = 0; i < accessories.getContainerSize(); i++) {
                var slotStack = accessories.getItem(i).copy();
                var slotReference = validContainer.createReference(i);

                if (slotStack.isEmpty() || !AccessoriesAPI.canUnequip(slotStack, slotReference)) continue;

                if (stack.isEmpty() || (AccessoriesAPI.canInsertIntoSlot(stack, slotReference) && extraCheck.isValid(slotStack, true))) {
                    return Pair.of(slotReference, (newStack) -> setStack(slotReference, newStack, true));
                }
            }
        }

        return null;
    }

    private Optional<ItemStack> setStack(SlotReference reference, ItemStack newStack, boolean shouldSwapStacks) {
        var oldStack = reference.getStack().copy();
        var accessory = AccessoriesAPI.getOrDefaultAccessory(oldStack);

        if(shouldSwapStacks) {
            var splitStack = newStack.isEmpty() ? ItemStack.EMPTY : newStack.split(accessory.maxStackSize(newStack));

            if (!entity.level().isClientSide) {
                reference.setStack(splitStack);
            }

            return Optional.of(oldStack);
        } else {
            if (!entity.level().isClientSide) {
                var splitStack = newStack.split(accessory.maxStackSize(newStack));

                reference.setStack(splitStack);
            }

            return Optional.empty();
        }
    }

    //--

    public SlotEntryReference getFirstEquipped(Predicate<ItemStack> predicate, EquipmentChecking check) {
        for (var container : this.getContainers().values()) {
            for (var stackEntry : container.getAccessories()) {
                var stack = stackEntry.getSecond();
                var reference = container.createReference(stackEntry.getFirst());

                if(check == EquipmentChecking.COSMETICALLY_OVERRIDABLE) {
                    var cosmetic = container.getCosmeticAccessories().getItem(reference.slot());

                    if(!cosmetic.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) stack = cosmetic;
                }

                var entryReference = AccessoryNestUtils.recursiveStackHandling(stack, reference, (innerStack, ref) -> {
                    return (!innerStack.isEmpty() && predicate.test(innerStack))
                            ? new SlotEntryReference(reference, innerStack)
                            : null;
                });

                if (entryReference != null) return entryReference;
            }
        }

        return null;
    }

    @Override
    public List<SlotEntryReference> getEquipped(Predicate<ItemStack> predicate) {
        return getAllEquipped().stream().filter(reference -> predicate.test(reference.stack())).toList();
    }

    @Override
    public List<SlotEntryReference> getAllEquipped(boolean recursiveStackLookup) {
        var references = new ArrayList<SlotEntryReference>();

        for (var container : this.getContainers().values()) {
            for (var stackEntry : container.getAccessories()) {
                var stack = stackEntry.getSecond();

                if (stack.isEmpty()) continue;

                var reference = container.createReference(stackEntry.getFirst());

                if(recursiveStackLookup) {
                    AccessoryNestUtils.recursiveStackConsumption(stack, reference, (innerStack, ref) -> references.add(new SlotEntryReference(ref, innerStack)));
                } else {
                    references.add(new SlotEntryReference(reference, stack));
                }
            }
        }

        return references;
    }

    @Override
    public void write(MapCarrier carrier, SerializationContext ctx) {
        this.holder().write(carrier, ctx);
    }

    @Override
    public void read(MapCarrier carrier, SerializationContext ctx) {
        this.holder().read(carrier, ctx);
    }
}
