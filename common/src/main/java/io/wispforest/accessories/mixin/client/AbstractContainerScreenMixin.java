package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ContainerScreenExtension {

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    private void accessories$isHoveringOverride(Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir){
        var override = this.isHovering_Logical(slot, mouseX, mouseY);

        if(override != null) cir.setReturnValue(override);
    }

    @Inject(method = "renderSlot", at = @At(value = "HEAD"), cancellable = true)
    private void accessories$shouldRenderSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        var result = this.shouldRenderSlot(slot);

        if(result != null && !result) ci.cancel();
    }

    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void accessories$adjustFor18x18(GuiGraphics instance, RenderPipeline pipeline, ResourceLocation texture, int x, int y, int width, int height, Operation<Void> original) {
        var atlas = Minecraft.getInstance().getAtlasManager()
            .getAtlasOrThrow(AtlasIds.GUI);

        var sprite = atlas.getSprite(texture);

        if (sprite != atlas.missingSprite()) {
            var ctn = sprite.contents();
            var is18x18 = ctn.width() == 18 && ctn.height() == 18;

            if(is18x18) {
                width = 18;
                height = 18;

                x = x - 1;
                y = y - 1;
            }
        }

        original.call(instance, pipeline, texture, x, y, width, height);
    }
}