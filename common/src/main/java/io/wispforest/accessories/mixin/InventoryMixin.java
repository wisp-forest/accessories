package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow @Final public Player player;

    @Inject(method = "clearOrCountMatchingItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", shift = At.Shift.AFTER))
    private void clearAccessories(Predicate<ItemStack> stackPredicate, int maxCount, Container inventory, CallbackInfoReturnable<Integer> cir, @Local(ordinal = 1) LocalIntRef i) {
        var capability = AccessoriesCapability.get(player);

        if(capability == null) return;

        capability.getContainers().forEach((s, container) -> {
            var accessories = container.getAccessories();
            i.set(i.get() + ContainerHelper.clearOrCountMatchingItems(accessories, stackPredicate, maxCount - i.get(), maxCount - i.get() == 0));

            var cosmetics = container.getCosmeticAccessories();
            i.set(i.get() + ContainerHelper.clearOrCountMatchingItems(cosmetics, stackPredicate, maxCount - i.get(), maxCount - i.get() == 0));
        });
    }

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("TAIL"))
    private void extendContainsCheck(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var capability = AccessoriesCapability.get(player);

        if (capability == null) return;

        var bl = capability.isEquipped(stack1 -> stack1.isEmpty() && ItemStack.isSameItemSameComponents(stack1, stack));

        if (bl) cir.setReturnValue(true);
    }

    @Inject(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("TAIL"))
    private void extendContainsCheck(TagKey<Item> tag, CallbackInfoReturnable<Boolean> cir){
        var capability = AccessoriesCapability.get(player);

        if(capability == null) return;

        var bl = capability.isEquipped(stack1 -> !stack1.isEmpty() && stack1.is(tag));

        if(bl) cir.setReturnValue(true);
    }

    @WrapOperation(method = "dropAll", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<List<ItemStack>> addAccessoriesToDropCall(List<List<ItemStack>> instance, Operation<Iterator<List<ItemStack>>> original) {
        var combinedList = new ArrayList<>(instance);

        combinedList.add(new ArrayList<>(((DroppedStacksExtension)this.player).toBeDroppedStacks()));

        return original.call(combinedList);
    }

    @Inject(method = "dropAll", at = @At(value = "TAIL"))
    private void addAccessoriesToDropCall(CallbackInfo ci) {
        ((DroppedStacksExtension)this.player).addToBeDroppedStacks(List.of());
    }
}