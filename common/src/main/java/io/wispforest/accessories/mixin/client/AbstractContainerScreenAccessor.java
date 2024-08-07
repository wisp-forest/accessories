package io.wispforest.accessories.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("clickedSlot") @Nullable Slot accessories$getClickedSlot();

    @Accessor("draggingItem") ItemStack accessories$getDraggingItem();

    @Accessor("isSplittingStack") boolean accessories$isSplittingStack();

    @Accessor("quickCraftingType") int accessories$getQuickCraftingType();

    @Invoker("recalculateQuickCraftRemaining") void accessories$recalculateQuickCraftRemaining();
}
