package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.pond.DroppedStacksExtension;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Accessor("player")
    public abstract Player accessories$player();

    @Inject(method = "clearOrCountMatchingItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", shift = At.Shift.AFTER))
    private void clearAccessories(Predicate<ItemStack> stackPredicate, int maxCount, Container inventory, CallbackInfoReturnable<Integer> cir, @Local(ordinal = 1) LocalIntRef i) {
        var capability = AccessoriesCapability.get(accessories$player());

        if(capability == null) return;

        capability.getContainers().forEach((s, container) -> {
            var accessories = container.getAccessories();
            i.set(i.get() + ContainerHelper.clearOrCountMatchingItems(accessories, stackPredicate, maxCount - i.get(), maxCount - i.get() == 0));

            var cosmetics = container.getCosmeticAccessories();
            i.set(i.get() + ContainerHelper.clearOrCountMatchingItems(cosmetics, stackPredicate, maxCount - i.get(), maxCount - i.get() == 0));
        });
    }

    @ModifyReturnValue(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("TAIL"))
    private boolean extendContainsCheck(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || checkAccessoriesContainers(stack1 -> stack1.isEmpty() && ItemStack.isSameItemSameComponents(stack1, stack));
    }

    @ModifyReturnValue(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("TAIL"))
    private boolean extendContainsCheck(boolean original, @Local(argsOnly = true) TagKey<Item> tag){
        return original || checkAccessoriesContainers(stack -> !stack.isEmpty() && stack.is(tag));
    }

    @ModifyReturnValue(method = "contains(Ljava/util/function/Predicate;)Z", at = @At("TAIL"))
    private boolean extendContainsCheck(boolean original, Predicate<ItemStack> predicate){
        return original || checkAccessoriesContainers(predicate);
    }

    @Unique
    private boolean checkAccessoriesContainers(Predicate<ItemStack> predicate){
        var capability = AccessoriesCapability.get(accessories$player());

        if(capability == null) return false;

        return capability.isEquipped(predicate);
    }

    @Inject(method = "dropAll", at = @At(value = "TAIL"))
    private void addAccessoriesToDropCall(CallbackInfo ci) {
        var player = accessories$player();
        var ext = ((DroppedStacksExtension) player);

        for (var itemstack : ext.toBeDroppedStacks()) {
            player.drop(itemstack, true, false);
        }

        ext.addToBeDroppedStacks(List.of());
    }
}