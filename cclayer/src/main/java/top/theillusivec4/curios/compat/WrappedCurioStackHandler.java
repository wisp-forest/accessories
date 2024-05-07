package top.theillusivec4.curios.compat;

import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WrappedCurioStackHandler implements ICurioStacksHandler {

    private final AccessoriesContainerImpl container;

    public WrappedCurioStackHandler(AccessoriesContainerImpl container){
        this.container = container;
    }

    @Override
    public IDynamicStackHandler getStacks() {
        return new HandlerImpl(this.container, false);
    }

    @Override
    public IDynamicStackHandler getCosmeticStacks() {
        return new HandlerImpl(this.container, true);
    }

    @Override
    public NonNullList<Boolean> getRenders() {
        return NonNullList.of(Boolean.TRUE, this.container.renderOptions().toArray(Boolean[]::new));
    }

    @Override
    public int getSlots() {
        return this.container.getSize();
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public boolean hasCosmetic() {
        return true;
    }

    @Override
    public CompoundTag serializeNBT() { return new CompoundTag(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) {}

    @Override
    public String getIdentifier() {
        return this.container.getSlotName();
    }

    @Override
    public Map<UUID, AttributeModifier> getModifiers() {
        return this.container.getModifiers();
    }

    @Override
    public Set<AttributeModifier> getPermanentModifiers() {
        return Set.of();
    }

    @Override
    public Set<AttributeModifier> getCachedModifiers() {
        return Set.of();
    }

    @Override
    public Collection<AttributeModifier> getModifiersByOperation(AttributeModifier.Operation operation) {
        return this.container.getModifiersForOperation(operation);
    }

    @Override
    public void addTransientModifier(AttributeModifier modifier) {
        this.container.addTransientModifier(modifier);
    }

    @Override
    public void addPermanentModifier(AttributeModifier modifier) {
        this.container.addTransientModifier(modifier);
    }

    @Override
    public void removeModifier(UUID uuid) {
        this.container.removeModifier(uuid);
    }

    @Override
    public void clearModifiers() {
        this.container.clearModifiers();
    }

    @Override
    public void clearCachedModifiers() {
        this.container.clearCachedModifiers();
    }

    @Override
    public void copyModifiers(ICurioStacksHandler other) {}

    @Override
    public void update() {}

    @Override
    public CompoundTag getSyncTag() {return new CompoundTag();}

    @Override
    public void applySyncTag(CompoundTag tag) {}

    @Override public int getSizeShift() { return 0; }
    @Override public void grow(int amount) {}
    @Override public void shrink(int amount) {}

    public static class HandlerImpl implements IDynamicStackHandler {
        public final AccessoriesContainer container;
        public final ExpandedSimpleContainer accessories;

        public final boolean isCosmetic;

        public final InvWrapper wrapper;

        public HandlerImpl(AccessoriesContainer container, boolean isCosmetic){
            this.container = container;
            this.accessories = (isCosmetic ? container.getCosmeticAccessories() : container.getAccessories());

            this.isCosmetic = isCosmetic;

            this.wrapper = new InvWrapper(accessories);
        }

        //--

        @Override public void setPreviousStackInSlot(int slot, @NotNull ItemStack stack) {}
        @Override public ItemStack getPreviousStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public void grow(int amount) {}
        @Override public void shrink(int amount) {}
        @Override public CompoundTag serializeNBT() { return new CompoundTag();}
        @Override public void deserializeNBT(CompoundTag nbt) {}

        //--

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            this.wrapper.setStackInSlot(slot, stack);
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.wrapper.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int i, ItemStack arg, boolean bl) {
            return this.wrapper.insertItem(i, arg, bl);
        }

        @Override
        public ItemStack extractItem(int i, int j, boolean bl) {
            return this.wrapper.extractItem(i, j, bl);
        }

        @Override
        public int getSlotLimit(int i) {
            return this.wrapper.getSlotLimit(i);
        }

        @Override
        public boolean isItemValid(int i, ItemStack arg) {
            return this.wrapper.isItemValid(i, arg);
        }

        @Override
        public int getSlots() {
            return this.wrapper.getSlots();
        }
    }
}
