package dev.emi.trinkets.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class TrinketInventory implements Container {
    private final SlotType slotType;
    private final int baseSize;
    private final TrinketComponent component;
    private final Map<ResourceLocation, AttributeModifier> modifiers = new HashMap<>();
    private final Set<AttributeModifier> persistentModifiers = new HashSet<>();
    private final Set<AttributeModifier> cachedModifiers = new HashSet<>();
    private final Multimap<AttributeModifier.Operation, AttributeModifier> modifiersByOperation = HashMultimap.create();
    private final Consumer<TrinketInventory> updateCallback;

    private NonNullList<ItemStack> stacks;
    private boolean update = false;

    public TrinketInventory(SlotType slotType, TrinketComponent comp, Consumer<TrinketInventory> updateCallback) {
        this.component = comp;
        this.slotType = slotType;
        this.baseSize = slotType.getAmount();
        this.stacks = NonNullList.withSize(this.baseSize, ItemStack.EMPTY);
        this.updateCallback = updateCallback;
    }

    public SlotType getSlotType() {
        return this.slotType;
    }

    public TrinketComponent getComponent() {
        return this.component;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        this.update();
        return this.stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (!stacks.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        this.update();
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(stacks, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(stacks, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.update();
        stacks.set(slot, stack);
    }

    @Override
    public void setChanged() {
        // NO-OP
    }

    public void markUpdate() {
        this.update = true;
        this.updateCallback.accept(this);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public Map<ResourceLocation, AttributeModifier> getModifiers() {
        return this.modifiers;
    }

    public Collection<AttributeModifier> getModifiersByOperation(AttributeModifier.Operation operation) {
        return this.modifiersByOperation.get(operation);
    }

    public void addModifier(AttributeModifier modifier) {
        this.modifiers.put(modifier.id(), modifier);
        this.getModifiersByOperation(modifier.operation()).add(modifier);
        this.markUpdate();
    }

    public void addPersistentModifier(AttributeModifier modifier) {
        this.addModifier(modifier);
        this.persistentModifiers.add(modifier);
    }

    public void removeModifier(ResourceLocation location) {
        AttributeModifier modifier = this.modifiers.remove(location);
        if (modifier != null) {
            this.persistentModifiers.remove(modifier);
            this.getModifiersByOperation(modifier.operation()).remove(modifier);
            this.markUpdate();
        }
    }

    public void clearModifiers() {
        Iterator<ResourceLocation> iter = this.getModifiers().keySet().iterator();

        while(iter.hasNext()) {
            this.removeModifier(iter.next());
        }
    }

    public void removeCachedModifier(AttributeModifier attributeModifier) {
        this.cachedModifiers.remove(attributeModifier);
    }

    public void clearCachedModifiers() {
        for (AttributeModifier cachedModifier : this.cachedModifiers) {
            this.removeModifier(cachedModifier.id());
        }
        this.cachedModifiers.clear();
    }

    public void update() {
        if (this.update) {
            this.update = false;
            double baseSize = this.baseSize;
            for (AttributeModifier mod : this.getModifiersByOperation(AttributeModifier.Operation.ADD_VALUE)) {
                baseSize += mod.amount();
            }

            double size = baseSize;
            for (AttributeModifier mod : this.getModifiersByOperation(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
                size += this.baseSize * mod.amount();
            }

            for (AttributeModifier mod : this.getModifiersByOperation(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
                size *= mod.amount();
            }
            LivingEntity entity = this.component.getEntity();

            if (size != this.getContainerSize()) {
                NonNullList<ItemStack> newStacks = NonNullList.withSize((int) size, ItemStack.EMPTY);
                for (int i = 0; i < this.stacks.size(); i++) {
                    ItemStack stack = this.stacks.get(i);
                    if (i < newStacks.size()) {
                        newStacks.set(i, stack);
                    } else {
                        entity.spawnAtLocation(stack);
                    }
                }

                this.stacks = newStacks;
            }
        }
    }

    public void copyFrom(TrinketInventory other) {
        this.modifiers.clear();
        this.modifiersByOperation.clear();
        this.persistentModifiers.clear();
        other.modifiers.forEach((uuid, modifier) -> this.addModifier(modifier));
        for (AttributeModifier persistentModifier : other.persistentModifiers) {
            this.addPersistentModifier(persistentModifier);
        }
        this.update();
    }

    public static void copyFrom(LivingEntity previous, LivingEntity current) {
        throw new IllegalStateException("API SHOULD NOT BE CALLED! [copyFrom]");
    }

    public CompoundTag toTag() {
        CompoundTag CompoundTag = new CompoundTag();
        if (!this.persistentModifiers.isEmpty()) {
            ListTag ListTag = new ListTag();
            for (AttributeModifier entityAttributeModifier : this.persistentModifiers) {
                ListTag.add(entityAttributeModifier.save());
            }

            CompoundTag.put("PersistentModifiers", ListTag);
        }
        if (!this.modifiers.isEmpty()) {
            ListTag ListTag = new ListTag();
            this.modifiers.forEach((uuid, modifier) -> {
                if (!this.persistentModifiers.contains(modifier)) {
                    ListTag.add(modifier.save());
                }
            });

            CompoundTag.put("CachedModifiers", ListTag);
        }
        return CompoundTag;
    }

    public void fromTag(CompoundTag tag) {
        if (tag.contains("PersistentModifiers", 9)) {
            ListTag ListTag = tag.getList("PersistentModifiers", 10);

            for(int i = 0; i < ListTag.size(); ++i) {
                AttributeModifier entityAttributeModifier = AttributeModifier.load(ListTag.getCompound(i));
                if (entityAttributeModifier != null) {
                    this.addPersistentModifier(entityAttributeModifier);
                }
            }
        }

        if (tag.contains("CachedModifiers", 9)) {
            ListTag ListTag = tag.getList("CachedModifiers", 10);

            for (int i = 0; i < ListTag.size(); ++i) {
                AttributeModifier entityAttributeModifier = AttributeModifier.load(ListTag.getCompound(i));
                if (entityAttributeModifier != null) {
                    this.cachedModifiers.add(entityAttributeModifier);
                    this.addModifier(entityAttributeModifier);
                }
            }

            this.update();
        }
    }

    public CompoundTag getSyncTag() {
        CompoundTag CompoundTag = new CompoundTag();
        if (!this.modifiers.isEmpty()) {
            ListTag ListTag = new ListTag();
            for (Map.Entry<ResourceLocation, AttributeModifier> modifier : this.modifiers.entrySet()) {
                ListTag.add(modifier.getValue().save());
            }

            CompoundTag.put("Modifiers", ListTag);
        }
        return CompoundTag;
    }

    public void applySyncTag(CompoundTag tag) {
        this.modifiers.clear();
        this.persistentModifiers.clear();
        this.modifiersByOperation.clear();
        if (tag.contains("Modifiers", 9)) {
            ListTag ListTag = tag.getList("Modifiers", 10);

            for (int i = 0; i < ListTag.size(); ++i) {
                AttributeModifier entityAttributeModifier = AttributeModifier.load(ListTag.getCompound(i));
                if (entityAttributeModifier != null) {
                    this.addModifier(entityAttributeModifier);
                }
            }
        }
        this.markUpdate();
        this.update();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrinketInventory that = (TrinketInventory) o;
        return slotType.equals(that.slotType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotType);
    }
}
