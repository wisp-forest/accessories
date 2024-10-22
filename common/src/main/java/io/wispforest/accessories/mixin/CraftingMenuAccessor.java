package io.wispforest.accessories.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CraftingMenu.class)
public interface CraftingMenuAccessor {
    @Invoker("slotChangedCraftingGrid")
    static void accessories$slotChangedCraftingGrid(AbstractContainerMenu abstractContainerMenu,
                                                    ServerLevel serverLevel,
                                                    Player player,
                                                    CraftingContainer craftingContainer,
                                                    ResultContainer resultContainer,
                                                    @Nullable RecipeHolder<CraftingRecipe> recipeHolder) {}
}
