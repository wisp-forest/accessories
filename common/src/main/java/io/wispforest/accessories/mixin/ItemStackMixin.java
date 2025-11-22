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
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackExtension {

    @Accessor("count")
    public abstract int accessories$count();

    @Nullable
    @Unique
    private EventStream<ItemStackResize> resizeEvent = null;

    @Override
    public EventStream<ItemStackResize> accessories$getResizeEvent() {
        if (this.resizeEvent == null) {
            this.resizeEvent = EnhancedEventStream.of((invokers, barrier) -> (stack, types) -> {
                try (barrier) {
                    invokers.forEach(invoker -> invoker.onResize(stack, types));
                }
            }, () -> this.resizeEvent = null);
        }

        return this.resizeEvent;
    }
    @WrapMethod(method = "setCount")
    private void accessories$handleResizeEvent(int count, Operation<Void> original) {
        int prevSize = 0;

        if (this.resizeEvent != null) prevSize = this.accessories$count();

        original.call(count);

        if (this.resizeEvent != null) accessories$getResizeEvent().sink().onResize((ItemStack) (Object) this, prevSize);
    }
}
