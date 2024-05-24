package top.theillusivec4.curios.compat;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record EmptyCuriosStackHandler(String identifier) implements ICurioStacksHandler {

    @Override public IDynamicStackHandler getStacks() { return new EmptyDynamicStackHandler(); }
    @Override public IDynamicStackHandler getCosmeticStacks() { return new EmptyDynamicStackHandler(); }
    @Override public NonNullList<Boolean> getRenders() { return NonNullList.create(); }
    @Override public int getSlots() { return 0; }
    @Override public boolean isVisible() { return false; }
    @Override public boolean hasCosmetic() { return true; }
    @Override public CompoundTag serializeNBT() { return new CompoundTag(); }
    @Override public void deserializeNBT(CompoundTag nbt) {}
    @Override public String getIdentifier() { return this.identifier; }
    @Override public Map<UUID, AttributeModifier> getModifiers() { return Map.of(); }
    @Override public Set<AttributeModifier> getPermanentModifiers() { return Set.of(); }
    @Override public Set<AttributeModifier> getCachedModifiers() { return Set.of(); }
    @Override public Collection<AttributeModifier> getModifiersByOperation(AttributeModifier.Operation operation) { return Set.of(); }
    @Override public void addTransientModifier(AttributeModifier modifier) {}
    @Override public void addPermanentModifier(AttributeModifier modifier) {}
    @Override public void removeModifier(UUID uuid) {}
    @Override public void clearModifiers() {}
    @Override public void clearCachedModifiers() {}
    @Override public void copyModifiers(ICurioStacksHandler other) {}
    @Override public void update() {}
    @Override public CompoundTag getSyncTag() { return new CompoundTag(); }
    @Override public void applySyncTag(CompoundTag tag) {}
    @Override public int getSizeShift() { return 0; }
    @Override public void grow(int amount) {}
    @Override public void shrink(int amount) {}

    public static class EmptyDynamicStackHandler implements IDynamicStackHandler {
        @Override public void setStackInSlot(int slot, @NotNull ItemStack stack) { /* NO-OP */ }
        @NotNull @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override @NotNull public ItemStack insertItem(int i, @NotNull ItemStack arg, boolean bl) { return ItemStack.EMPTY; }
        @Override @NotNull public ItemStack extractItem(int i, int j, boolean bl) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int i) { return 0; }
        @Override public boolean isItemValid(int i, @NotNull ItemStack arg) { return false; }
        @Override public void setPreviousStackInSlot(int slot, @NotNull ItemStack stack) { /* NO-OP */ }
        @Override public ItemStack getPreviousStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public int getSlots() { return 0;}
        @Override public void grow(int amount) {}
        @Override public void shrink(int amount) {}
        @Override public CompoundTag serializeNBT() { return new CompoundTag(); }
        @Override public void deserializeNBT(CompoundTag nbt) {}
    }
}
