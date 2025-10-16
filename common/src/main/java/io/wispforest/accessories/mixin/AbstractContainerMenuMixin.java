package io.wispforest.accessories.mixin;

import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Shadow public abstract void setCarried(ItemStack stack);

    @Shadow public abstract ItemStack getCarried();

    @Inject(method = "transferState", at = @At("HEAD"))
    private void accessories$transferCarriedStack(AbstractContainerMenu menu, CallbackInfo ci) {
        if (menu instanceof AccessoriesMenuBase base) {
            this.setCarried(base.getTempCarriedStack());
        }
    }
}
