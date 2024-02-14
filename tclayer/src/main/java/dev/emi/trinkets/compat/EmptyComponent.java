package dev.emi.trinkets.compat;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class EmptyComponent implements TrinketComponent {

    @Override
    public LivingEntity getEntity() {
        return null;
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return null;
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        return null;
    }

    @Override
    public void update() {}

    @Override
    public void addTemporaryModifiers(Multimap<String, AttributeModifier> modifiers) {}

    @Override
    public void addPersistentModifiers(Multimap<String, AttributeModifier> modifiers) {}

    @Override
    public void removeModifiers(Multimap<String, AttributeModifier> modifiers) {}

    @Override
    public void clearModifiers() {}

    @Override
    public Multimap<String, AttributeModifier> getModifiers() {
        return null;
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        return false;
    }

    @Override
    public List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        return null;
    }

    @Override
    public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {}

    @Override
    public Set<TrinketInventory> getTrackingUpdates() {
        return null;
    }

    @Override
    public void clearCachedModifiers() {}

    @Override
    public void readFromNbt(CompoundTag tag) {}

    @Override
    public void writeToNbt(CompoundTag tag) {}
}
