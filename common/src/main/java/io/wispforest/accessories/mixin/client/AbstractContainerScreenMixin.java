package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ContainerScreenExtension {

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void accessories$isHoveringOverride(Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir){
        var override = this.isHovering_Logical(slot, mouseX, mouseY);

        if(override != null) cir.setReturnValue(override);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isHighlightable()Z"))
    private boolean accessories$isHoveringOverride(Slot slot, Operation<Boolean> original, @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY){
        var override = this.isHovering_Rendering(slot, mouseX, mouseY);

        return (override == null)
                ? original.call(slot)
                : override;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V"))
    private void accessories$shouldRenderSlot(AbstractContainerScreen instance, GuiGraphics guiGraphics, Slot slot, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
        var result = this.shouldRenderSlot(slot, mouseX, mouseY);

        if(result != null && !result) return;

        original.call(instance, guiGraphics, slot);
    }
}
