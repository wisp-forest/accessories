package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.api.client.tooltip.DeferredTooltip;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.pond.DefaultTooltipPositionerExt;
import io.wispforest.accessories.pond.DeferredTooltipGetter;
import io.wispforest.accessories.pond.ScissorStackManipulation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements ScissorStackManipulation, DeferredTooltipGetter {

    @Accessor("scissorStack")
    public abstract GuiGraphics.ScissorStack accessories$scissorStack();

    @Shadow
    private @Nullable Runnable deferredTooltip;

    @Override
    public void accessories$renderWithoutEntries(Runnable runnable, @Nullable Integer levels) {
        ((ScissorStackManipulation) this.accessories$scissorStack()).accessories$renderWithoutEntries(runnable, levels);
    }

    @WrapMethod(method = "setTooltipForNextFrameInternal")
    private void accessories$adjustPositioner(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, @Nullable ResourceLocation background, boolean focused, Operation<Void> original) {
        // TODO: MAYBE GOOD IDEA TO CHECK AGAINST REFERENCE OF THE SINGULAR USED INSTANCE
        if (positioner instanceof DefaultTooltipPositioner defaultTooltipPositioner) {
            var basePositioner = AccessoriesScreenBase.ALTERATIVE_POSITIONER.getValue();

            if (basePositioner != null) {
                positioner = DefaultTooltipPositionerExt.copyWith(defaultTooltipPositioner, basePositioner);
            }
        }

        original.call(font, components, x, y, positioner, background, focused);
    }

    @WrapOperation(method = "setTooltipForNextFrameInternal", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiGraphics;deferredTooltip:Ljava/lang/Runnable;", opcode = Opcodes.PUTFIELD))
    private void accessories$wrapTooltipRenderCall(GuiGraphics instance, Runnable value, Operation<Void> original,
                      @Local(argsOnly = true) Font font,
                      @Local(argsOnly = true) List<ClientTooltipComponent> components,
                      @Local(argsOnly = true, ordinal = 0) int x,
                      @Local(argsOnly = true, ordinal = 1) int y,
                      @Local(argsOnly = true) ClientTooltipPositioner positioner,
                      @Local(argsOnly = true) @Nullable ResourceLocation background,
                      @Local(argsOnly = true) boolean focused) {
        original.call(instance, new DeferredTooltip(font, components, x, y, positioner, background, focused, value));
    }

    @Override
    public @Nullable DeferredTooltip accessories$getTooltip() {
        return this.deferredTooltip instanceof DeferredTooltip tooltip ? tooltip : null;
    }

    @Mixin(GuiGraphics.ScissorStack.class)
    public abstract static class ScissorStackMixin implements ScissorStackManipulation {
        @Accessor("stack")
        public abstract Deque<ScreenRectangle> accessories$stack();

        @Override
        public void accessories$renderWithoutEntries(Runnable runnable, @Nullable Integer levels) {
            var originalStack = accessories$stack();
            var copiedStack = new ArrayDeque<>(accessories$stack());

            if (levels != null) {
                for (var i = 0; i < levels; i++) {
                    originalStack.pollLast();
                }

                runnable.run();

                originalStack.clear();
            } else {
                originalStack.clear();

                runnable.run();
            }

            originalStack.addAll(copiedStack);
        }
    }
}
