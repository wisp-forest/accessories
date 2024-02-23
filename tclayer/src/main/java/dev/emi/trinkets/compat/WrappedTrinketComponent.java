package dev.emi.trinkets.compat;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.*;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class WrappedTrinketComponent implements TrinketComponent {

    private final AccessoriesCapability capability;

    public WrappedTrinketComponent(AccessoriesCapability capability){
        this.capability = capability;
    }

    @Override
    public LivingEntity getEntity() {
        return capability.getEntity();
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return TrinketsApi.getEntitySlots(getEntity().level(), getEntity().getType());
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        //TODO: HANDLE SUCH TRINKET SPECIFIC NAMES AND GROUPS
        var groups = SlotGroupLoader.INSTANCE.getGroups(capability.getEntity().level().isClientSide());
        var containers = capability.getContainers();

        var inventories = new HashMap<String, Map<String, TrinketInventory>>();

        for (var entry : groups.entrySet()) {
            var map = new HashMap<String, TrinketInventory>();

            entry.getValue().slots().forEach(s -> {
                var container = containers.get(s);

                if(container == null) return;

                var wrappedInv = new WrappedTrinketInventory(WrappedTrinketComponent.this, container, SlotTypeLoader.getSlotType(capability.getEntity().level(), (String) s).get());

                map.put(s, wrappedInv);
            });

            inventories.put(entry.getKey(), map);
        }

        return inventories;
    }

    @Override
    public void update() {}

    @Override
    public void addTemporaryModifiers(Multimap<String, AttributeModifier> modifiers) {
        capability.addTransientSlotModifiers(modifiers);
    }

    @Override
    public void addPersistentModifiers(Multimap<String, AttributeModifier> modifiers) {
        capability.addPersistentSlotModifiers(modifiers);
    }

    @Override
    public void removeModifiers(Multimap<String, AttributeModifier> modifiers) {
        capability.removeSlotModifiers(modifiers);
    }

    @Override
    public void clearModifiers() {
        capability.clearSlotModifiers();
    }

    @Override
    public Multimap<String, AttributeModifier> getModifiers() {
        return capability.getSlotModifiers();
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        return capability.isEquipped(predicate);
    }

    @Override
    public List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        var equipped = capability.getEquipped(predicate);

        return equipped.stream()
                .map(slotResult -> {
                    var reference = WrappingTrinketsUtils.createReference(slotResult.reference());

                    return reference.map(slotReference -> new Tuple<>(
                            slotReference,
                            slotResult.stack()
                    )).orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {
        var equipped = capability.getEquipped(stack -> true);

        equipped.forEach(slotResult -> {
            var reference = WrappingTrinketsUtils.createReference(slotResult.reference());

            if(reference.isEmpty()) return;

            consumer.accept(
                    reference.get(),
                    slotResult.stack()
            );
        });
    }

    @Override
    public Set<TrinketInventory> getTrackingUpdates() {
        return null;
    }

    @Override
    public void clearCachedModifiers() {
        capability.clearCachedSlotModifiers();
    }

    @Override public void readFromNbt(CompoundTag tag) {}
    @Override public void writeToNbt(CompoundTag tag) {}
}
