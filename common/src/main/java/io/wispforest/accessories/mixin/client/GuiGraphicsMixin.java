package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.pond.ScissorStackManipulation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements ScissorStackManipulation {

    @Accessor("scissorStack")
    public abstract GuiGraphics.ScissorStack accessories$scissorStack();

    @Override
    public void accessories$renderWithoutEntries(Runnable runnable, @Nullable Integer levels) {
        ((ScissorStackManipulation) this.accessories$scissorStack()).accessories$renderWithoutEntries(runnable, levels);
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
