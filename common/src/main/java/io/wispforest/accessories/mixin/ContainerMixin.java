package io.wispforest.accessories.mixin;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(Container.class)
public interface ContainerMixin {

    @Inject(method = "hasAnyMatching", at = @At("TAIL"))
    private void extendHasAnyMatching(Predicate<ItemStack> predicate, CallbackInfoReturnable<Boolean> cir) {
        if (!(this instanceof Inventory inventory)) return;

        var capability = AccessoriesCapability.get(inventory.player);

        if (capability == null) return;

        var bl = capability.isEquipped(predicate);

        if (bl) cir.setReturnValue(true);
    }
}
