package io.wispforest.accessories.mixin.owo;

import io.wispforest.owo.config.Option;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Option.class)
public interface OptionAccessor<T> {
    @Invoker("read")
    T accessories$read(FriendlyByteBuf buf);

    @Invoker("write")
    void accessories$write(FriendlyByteBuf buf);
}
