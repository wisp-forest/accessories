package dev.emi.trinkets.compat;

import com.google.common.collect.HashMultimap;
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

    private final LivingEntity livingEntity;

    public EmptyComponent(LivingEntity livingEntity){
        this.livingEntity = livingEntity;
    }

    @Override
    public LivingEntity getEntity() {
        return livingEntity;
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return Map.of();
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        return Map.of();
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
        return HashMultimap.create();
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        return false;
    }

    @Override
    public List<Tuple<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        return List.of();
    }

    @Override
    public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {}

    @Override
    public Set<TrinketInventory> getTrackingUpdates() {
        return Set.of();
    }

    @Override
    public void clearCachedModifiers() {}

    @Override
    public void readFromNbt(CompoundTag tag) {}

    @Override
    public void writeToNbt(CompoundTag tag) {}
}
