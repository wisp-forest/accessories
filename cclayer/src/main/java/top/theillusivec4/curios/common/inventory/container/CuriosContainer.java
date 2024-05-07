/*
 * Copyright (c) 2018-2020 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.theillusivec4.curios.common.inventory.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ICuriosMenu;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import javax.annotation.Nonnull;

public class CuriosContainer extends InventoryMenu implements ICuriosMenu {

    private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[] {
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
    private static final EquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};

    public final LazyOptional<ICuriosItemHandler> curiosHandler;
    public final Player player;

    private final boolean isLocalWorld;

    private final CraftingContainer craftMatrix = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer craftResult = new ResultContainer();
    private int lastScrollIndex;
    private boolean cosmeticColumn;
    private boolean skip = false;

    public CuriosContainer(int windowId, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
        this(windowId, playerInventory);
    }

    public CuriosContainer(int windowId, Inventory playerInventory) {
        this(windowId, playerInventory, false);
    }

    public CuriosContainer(int windowId, Inventory playerInventory, boolean skip) {
        super(playerInventory, playerInventory.player.level().isClientSide, playerInventory.player);
        this.slots.clear();
        this.player = playerInventory.player;
        this.isLocalWorld = this.player.level().isClientSide;
        this.curiosHandler = CuriosApi.getCuriosInventory(this.player);
    }

    public boolean hasCosmeticColumn() {
        return this.cosmeticColumn;
    }

    public void resetSlots() {
        this.scrollToIndex(this.lastScrollIndex);
    }

    public void scrollToIndex(int indexIn) {
        // NO-OP
    }

    public void scrollTo(float pos) {
        // NO-OP
    }

    @Override
    public void slotsChanged(@Nonnull Container inventoryIn) {
        // NO-OP
    }

    @Override
    public void removed(@Nonnull Player playerIn) {
        // NO-OP
    }

    public boolean canScroll() {
        // NO-OP
        return false;
    }

    @Override
    public boolean stillValid(@Nonnull Player playerIn) {

        return true;
    }

    @Override
    public void setItem(int pSlotId, int pStateId, @Nonnull ItemStack pStack) {
        // NO-OP
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player playerIn, int index) {
        // NO-OP
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public RecipeBookType getRecipeBookType() {
        // NO-OP
        return RecipeBookType.CRAFTING;
    }

    @Override
    public void fillCraftSlotsStackedContents(@Nonnull StackedContents itemHelperIn) {
        // NO-OP
        this.craftMatrix.fillStackedContents(itemHelperIn);
    }

    @Override
    public void clearCraftingContent() {
        // NO-OP
        this.craftMatrix.clearContent();
        this.craftResult.clearContent();
    }

    @Override
    public boolean recipeMatches(Recipe<? super CraftingContainer> recipeIn) {
        // NO-OP
        return recipeIn.matches(this.craftMatrix, this.player.level());
    }

    @Override
    public int getGridWidth() {
        // NO-OP
        return this.craftMatrix.getWidth();
    }

    @Override
    public int getGridHeight() {
        // NO-OP
        return this.craftMatrix.getHeight();
    }

}