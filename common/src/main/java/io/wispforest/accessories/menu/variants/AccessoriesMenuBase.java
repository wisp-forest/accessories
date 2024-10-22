package io.wispforest.accessories.menu.variants;

import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.mixin.CraftingMenuAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.server.ScreenOpen;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AccessoriesMenuBase extends AbstractCraftingMenu {

    protected final Player owner;

    @Nullable
    protected final LivingEntity targetEntity;

    protected AccessoriesMenuBase(MenuType<? extends AccessoriesMenuBase> menuType, int containerId, Inventory inventory, int width, int height, @Nullable LivingEntity targetEntity) {
        super(menuType, containerId, width, height);

        this.owner = inventory.player;
        this.targetEntity = targetEntity;

        if (this instanceof AccessoriesExperimentalMenu) {
            this.addResultSlot(inventory.player, 154, 28);
            this.addCraftingGridSlots(98, 18);
        }
    }

    public final AccessoriesMenuVariant menuVariant() {
        return AccessoriesMenuVariant.getVariant((MenuType<? extends AccessoriesMenuBase>) this.getType());
    }

    private static final Slot EMPTY_SLOT = new Slot(new SimpleContainer(1), 0, 0, 0);

    @Override
    public Slot getResultSlot() {
        return (this instanceof AccessoriesExperimentalMenu) ? this.slots.get(0) : EMPTY_SLOT;
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return (this instanceof AccessoriesExperimentalMenu) ? this.slots.subList(1, 5) : List.of();
    }

    @Nullable
    public final LivingEntity targetEntity() {
        return this.targetEntity;
    }

    public final Player owner() {
        return this.owner;
    }

    public final void reopenMenu() {
        AccessoriesNetworking.sendToServer(ScreenOpen.of(this.targetEntity(), this.menuVariant()));
    }

    //--

    @Nullable
    public Pair<ItemStack, ItemStack> quickMoveStackCrafting(int index) {
        var slot = this.slots.get(index);

        if (slot.hasItem()) {
            var itemStack2 = slot.getItem();
            var itemStack = itemStack2.copy();

            var endIndex = 5 + (4 * 9);

            if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 5, endIndex, true)) return Pair.of(ItemStack.EMPTY, null);

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index >= 1 && index < 5) {
                if (!this.moveItemStackTo(itemStack2, 5, endIndex, false)) return Pair.of(ItemStack.EMPTY, null);
            }

            if (itemStack2.getCount() == itemStack.getCount()) return null;

            return Pair.of(itemStack, itemStack2);
        }

        return null;
    }

    public void fillCraftSlotsStackedContents(StackedItemContents stackedItemContents) {
        this.craftSlots.fillStackedContents(stackedItemContents);
    }

    public void clearCraftingContent() {
        this.resultSlots.clearContent();
        this.craftSlots.clearContent();
    }

    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().matches(this.craftSlots.asCraftInput(), this.owner.level());
    }

    public void slotsChanged(Container container) {
        if (!(this.owner.level() instanceof ServerLevel serverLevel)) return;
        
        CraftingMenuAccessor.accessories$slotChangedCraftingGrid(this, serverLevel, this.owner, this.craftSlots, this.resultSlots, (RecipeHolder) null);
    }

    public void removed(Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.craftSlots);
        }
    }

    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    public int getResultSlotIndex() {
        return -1;
    }

    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    public int getSize() {
        return this.craftSlots.getWidth() * this.craftSlots.getHeight() + 1;
    }

    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != this.getResultSlotIndex();
    }
}
