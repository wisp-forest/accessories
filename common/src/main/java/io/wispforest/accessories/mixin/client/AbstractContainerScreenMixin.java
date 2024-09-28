package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ContainerScreenExtension {

    @Shadow protected abstract void renderSlot(GuiGraphics guiGraphics, Slot slot);

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void accessories$isHoveringOverride(Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir){
        var override = this.isHovering_Logical(slot, mouseX, mouseY);

        if(override != null) cir.setReturnValue(override);
    }

    @Inject(method = "renderSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"), cancellable = true)
    private void accessories$shouldRenderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if(accessories$bypassSlotCheck) return;

        var result = this.shouldRenderSlot(slot);

        if(result != null && !result) ci.cancel();
    }

    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"))
    private void accessories$adjustFor18x18(GuiGraphics instance, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, Operation<Void> original) {
        var is18x18 = sprite.contents().width() == 18 && sprite.contents().height() == 18;

        if(is18x18) {
            width = 18;
            height = 18;

            x = x - 1;
            y = y - 1;
        }

        original.call(instance, x, y, blitOffset, width, height, sprite);
    }

    @Unique
    private boolean accessories$bypassSlotCheck = false;

    @Override
    public void forceRenderSlot(GuiGraphics context, Slot slot) {
        this.accessories$bypassSlotCheck = true;

        this.renderSlot(context, slot);

        this.accessories$bypassSlotCheck = false;
    }
}