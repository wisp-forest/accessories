package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

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

    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIII)V"))
    private void accessories$adjustFor18x18(GuiGraphics instance, Function<ResourceLocation, RenderType> function, TextureAtlasSprite textureAtlasSprite, int x, int y, int width, int height, Operation<Void> original) {
        var is18x18 = textureAtlasSprite.contents().width() == 18 && textureAtlasSprite.contents().height() == 18;

        if(is18x18) {
            width = 18;
            height = 18;

            x = x - 1;
            y = y - 1;
        }

        original.call(instance, function, textureAtlasSprite, x, y, width, height);
    }

    @WrapOperation(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void accessories$adjustZOffset(PoseStack instance, float x, float y, float z, Operation<Void> original) {
        original.call(instance, x, y, z + this.hoverStackOffset());
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