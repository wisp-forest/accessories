package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.pond.stack.ItemStackExtension;
import io.wispforest.accessories.utils.EnhancedEventStream;
import io.wispforest.accessories.utils.ItemStackResize;
import io.wispforest.owo.util.EventStream;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public class ItemStackMixin implements ItemStackExtension {

    @Shadow private int count;

    @Nullable
    @Unique
    private EventStream<ItemStackResize> resizeEvent = null;

    @Override
    public EventStream<ItemStackResize> accessories$getResizeEvent() {
        if (resizeEvent == null) {
            resizeEvent = EnhancedEventStream.of((invokers, barrier) -> (stack, types) -> {
                try (barrier) {
                    invokers.forEach(invoker -> invoker.onResize(stack, types));
                }
            }, () -> resizeEvent = null);
        }

        return resizeEvent;
    }
    @WrapMethod(method = "setCount")
    private void accessories$handleResizeEvent(int count, Operation<Void> original) {
        int prevSize = 0;

        if (resizeEvent != null) {
            prevSize = this.count;
        }

        original.call(count);

        if (resizeEvent != null) {
            accessories$getResizeEvent().sink().onResize((ItemStack) (Object) this, prevSize);
        }
    }
}
