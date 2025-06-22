package io.wispforest.accessories.utils;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ImmutableContainer implements Container {

    private final List<ItemStack> stacks;

    public ImmutableContainer(List<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return stacks.get(slot);
    }

    //--

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override public void setItem(int slot, ItemStack stack) { /* NO-OP */ }
    @Override public void setChanged() { /* NO-OP */ }
    @Override public void clearContent() { /* NO-OP */ }
}
