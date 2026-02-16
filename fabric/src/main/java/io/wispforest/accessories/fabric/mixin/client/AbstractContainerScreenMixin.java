package io.wispforest.accessories.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ContainerScreenExtension {

    @Shadow @Nullable protected Slot hoveredSlot;

    @WrapOperation(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void accessories$isHoveringOverrideFront(AbstractContainerScreen instance, GuiGraphics guiGraphics, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY){
        var override = this.isHovering_Rendering(this.hoveredSlot, mouseX, mouseY);

        if (override == null || override) original.call(instance, guiGraphics);
    }

    @WrapOperation(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void accessories$isHoveringOverrideBack(AbstractContainerScreen instance, GuiGraphics guiGraphics, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY){
        var override = this.isHovering_Rendering(this.hoveredSlot, mouseX, mouseY);

        if (override == null || override) original.call(instance, guiGraphics);
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;matches(Lnet/minecraft/client/input/KeyEvent;)Z", ordinal = 0))
    private boolean accessories$adjustCloseCheck(KeyMapping instance, KeyEvent arg, Operation<Boolean> original) {
        return original.call(instance, arg) || AccessoriesClient.isInventoryKey(keyMapping -> original.call(keyMapping, arg));
    }
}