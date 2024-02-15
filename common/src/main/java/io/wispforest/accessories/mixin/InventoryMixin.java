package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import io.wispforest.accessories.AccessoriesAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ClearInventoryCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.function.Predicate;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "clearOrCountMatchingItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", shift = At.Shift.AFTER))
    private void clearAccessories(Predicate<ItemStack> stackPredicate, int maxCount, Container inventory, CallbackInfoReturnable<Integer> cir, @Local(ordinal = 1) LocalIntRef i) {
        var accessories = AccessoriesAccess.getCapability(player).get();
        if (accessories == null) return;
        accessories.getContainers().forEach((s, accessoriesContainer) -> {
            for (int accessoryIndex = 0; accessoryIndex < accessoriesContainer.getAccessories().getContainerSize(); accessoryIndex++) {
                i.set(i.get() + ContainerHelper.clearOrCountMatchingItems(accessoriesContainer.getAccessories(), stackPredicate, maxCount - i.get(), maxCount - i.get() == 0));
            }
        });
    }
}