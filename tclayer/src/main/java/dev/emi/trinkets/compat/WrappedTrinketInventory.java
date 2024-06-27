package dev.emi.trinkets.compat;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class WrappedTrinketInventory extends TrinketInventory {

    public final AccessoriesContainer container;

    public WrappedTrinketInventory(TrinketComponent component, AccessoriesContainer container, SlotType slotType) {
        super(new WrappedSlotType(slotType, container.capability().entity().level().isClientSide()), component, trinketInventory -> {});

        this.container = (AccessoriesContainer) container;
    }

    @Override
    public Map<UUID, AttributeModifier> getModifiers() {
        return container.getModifiers();
    }

    @Override
    public Collection<AttributeModifier> getModifiersByOperation(AttributeModifier.Operation operation) {
        return container.getModifiersForOperation(operation);
    }

    @Override
    public void addModifier(AttributeModifier modifier) {
        container.addTransientModifier(modifier);
    }

    @Override
    public void addPersistentModifier(AttributeModifier modifier) {
        container.addPersistentModifier(modifier);
    }

    @Override
    public void removeModifier(UUID uuid) {
        container.removeModifier(uuid);
    }

    @Override
    public void clearModifiers() {
        container.clearModifiers();
    }

    @Override
    public void removeCachedModifier(AttributeModifier attributeModifier) {
        container.getCachedModifiers().remove(attributeModifier);
    }

    @Override
    public void clearCachedModifiers() {
        container.clearCachedModifiers();
    }

    @Override
    public void markUpdate() {
        container.markChanged(false);
    }

    //--

    @Override
    public void clearContent() {
        var accessories = container.getAccessories();
        var cosmetics = container.getCosmeticAccessories();

        for (int i = 0; i < accessories.getContainerSize(); i++) {
            accessories.setItem(i, ItemStack.EMPTY);
            cosmetics.setItem(i, ItemStack.EMPTY);
        }

        this.markUpdate();
    }

    @Override
    public int getContainerSize() {
        return container.getAccessories().getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return container.getAccessories().getContainerSize() != 0;
    }

    @Override
    public ItemStack getItem(int slot) {
        return container.getAccessories().getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return container.getAccessories().removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        var stacks = container.getAccessories();

        var itemStack = stacks.getItem(slot);
        stacks.setItem(slot, ItemStack.EMPTY);

        return itemStack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        container.getAccessories().setItem(slot, stack);
    }

    //--
}
