package dev.emi.trinkets.compat;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.*;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public abstract class WrappedTrinketComponent implements TrinketComponent {

    protected final AccessoriesCapability capability;

    public WrappedTrinketComponent(AccessoriesCapability capability){
        this.capability = capability;
    }

    @Override
    public LivingEntity getEntity() {
        return capability.entity();
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return TrinketsApi.getEntitySlots(getEntity().level(), getEntity().getType());
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        //TODO: HANDLE SUCH TRINKET SPECIFIC NAMES AND GROUPS
        var groups = SlotGroupLoader.INSTANCE.getAllGroups(capability.entity().level().isClientSide());
        var containers = capability.getContainers();

        var inventories = new HashMap<String, Map<String, TrinketInventory>>();

        for (var entry : groups.entrySet()) {
            var map = new HashMap<String, TrinketInventory>();

            entry.getValue().slots().forEach(s -> {
                var container = containers.get(s);

                if(container == null) return;

                var wrappedInv = new WrappedTrinketInventory(WrappedTrinketComponent.this, container, SlotTypeLoader.getSlotType(capability.entity().level(), (String) s));

                map.put(WrappingTrinketsUtils.accessoriesToTrinkets_Slot(s), wrappedInv);
            });

            inventories.put(WrappingTrinketsUtils.accessoriesToTrinkets_Group(entry.getKey()), map);
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

    @Override
    public void readFromNbt(CompoundTag tag) {
        if(tag.getBoolean("is_accessories_data")) {
            ((AccessoriesHolderImpl)this.capability.getHolder())
                    .read(tag.getCompound("main_data"));
        } else {
            var dropped = new ArrayList<ItemStack>();

            for (var groupKey : tag.getAllKeys()) {
                var groupTag = tag.getCompound(groupKey);

                for (var slotKey : groupTag.getAllKeys()) {
                    var slotTag = groupTag.getCompound(slotKey);

                    var slotName = WrappingTrinketsUtils.trinketsToAccessories_Slot(slotKey);

                    var slotType = SlotTypeLoader.getSlotType(this.getEntity().level(), slotName);

                    var list = slotTag.getList("Items", NbtType.COMPOUND)
                            .stream()
                            .map(tagEntry -> ItemStack.of((tagEntry instanceof CompoundTag compoundTag) ? compoundTag : new CompoundTag()))
                            .toList();

                    if (slotType == null) {
                        dropped.addAll(list);

                        continue;
                    }

                    var container = this.capability.getContainer(slotType);

                    if (container == null) {
                        dropped.addAll(list);

                        System.out.println("Unable to handle the given slotType as a container did not exist");

                        continue;
                    }

                    var accessories = container.getAccessories();

                    for (var stack : list) {
                        boolean consumedStack = false;

                        for (int i = 0; i < accessories.getContainerSize() && !consumedStack; i++) {
                            var currentStack = accessories.getItem(i);

                            if (!currentStack.isEmpty()) continue;

                            var ref = io.wispforest.accessories.api.slot.SlotReference.of(this.getEntity(), slotName, i);

                            if (!AccessoriesAPI.canInsertIntoSlot(stack, ref)) continue;

                            accessories.setItem(i, stack.copy());

                            consumedStack = true;
                        }

                        if (!consumedStack) {
                            dropped.add(stack.copy());
                        }
                    }
                }
            }

            ((AccessoriesHolderImpl) this.capability.getHolder()).invalidStacks.addAll(dropped);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        var innerCompound = new CompoundTag();

        ((AccessoriesHolderImpl)this.capability.getHolder())
                .write(innerCompound);

        tag.put("main_data", innerCompound);
        tag.putBoolean("is_accessories_data", true);
    }
}
