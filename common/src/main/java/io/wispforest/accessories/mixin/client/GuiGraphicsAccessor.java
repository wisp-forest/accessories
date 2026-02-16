package io.wispforest.accessories.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsAccessor {
    @Invoker("setTooltipForNextFrameInternal")
    void accessories$setTooltipForNextFrameInternal(
        Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, @Nullable Identifier background, boolean focused
    );
}
