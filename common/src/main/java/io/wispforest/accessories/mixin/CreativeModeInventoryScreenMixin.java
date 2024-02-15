package io.wispforest.accessories.mixin;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.networking.server.NukeAccessories;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Inject(method = "slotClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I", ordinal = 0, shift = At.Shift.AFTER))
    private void clearAccessoriesWithClearSlot(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        AccessoriesAccess.getNetworkHandler().sendToServer(new NukeAccessories());
    }
}