package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.pond.ScissorStackManipulation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements ScissorStackManipulation {

    @Shadow @Final public GuiGraphics.ScissorStack scissorStack;

    @Override
    public void accessories$renderWithoutEntries(Runnable runnable, @Nullable Integer levels) {
        ((ScissorStackManipulation) this.scissorStack).accessories$renderWithoutEntries(runnable, levels);
    }

    @Mixin(GuiGraphics.ScissorStack.class)
    public abstract static class ScissorStackMixin implements ScissorStackManipulation {
        @Shadow @Final private Deque<ScreenRectangle> stack;

        @Override
        public void accessories$renderWithoutEntries(Runnable runnable, @Nullable Integer levels) {
            Deque<ScreenRectangle> stackCopy = new ArrayDeque<>(stack);

            if (levels != null) {
                for (var i = 0; i < levels; i++) {
                    stack.pollLast();
                }

                runnable.run();

                stack.clear();
            } else {
                stack.clear();

                runnable.run();
            }

            stack.addAll(stackCopy);
        }
    }
}
