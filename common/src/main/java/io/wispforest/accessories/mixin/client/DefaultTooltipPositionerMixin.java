package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.client.gui.AccessoriesScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefaultTooltipPositioner.class)
public abstract class DefaultTooltipPositionerMixin {

    @Inject(method = "positionTooltip(IILorg/joml/Vector2i;II)V", at = @At(value = "HEAD"))
    private void accessories$forceLeftPositioning(int screenWidth, int screenHeight, Vector2i tooltipPos, int tooltipWidth, int tooltipHeight, CallbackInfo ci) {
        if (tooltipPos.x + tooltipWidth <= screenWidth && AccessoriesScreen.FORCE_TOOLTIP_LEFT) {
            tooltipPos.x = Math.max(tooltipPos.x - 24 - tooltipWidth, 4);
        }
    }
}