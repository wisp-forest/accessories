package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
        var result = this.shouldRenderSlot(slot);

        if(result != null && !result) ci.cancel();
    }

    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void accessories$adjustFor18x18(GuiGraphics instance, Function<ResourceLocation, RenderType> function, ResourceLocation texture, int x, int y, int width, int height, Operation<Void> original) {
        var textureAtlasSprite = Minecraft.getInstance().getGuiSprites().getSprite(texture);

        var is18x18 = textureAtlasSprite.contents().width() == 18 && textureAtlasSprite.contents().height() == 18;

        if(is18x18) {
            width = 18;
            height = 18;

            x = x - 1;
            y = y - 1;
        }

        original.call(instance, function, texture, x, y, width, height);
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;matches(II)Z"))
    private boolean accessories$adjustCloseCheck(KeyMapping instance, int keysym, int scancode, Operation<Boolean> original) {
        return original.call(instance, keysym, scancode) || original.call(AccessoriesClient.OPEN_SCREEN, keysym, scancode);
    }
}