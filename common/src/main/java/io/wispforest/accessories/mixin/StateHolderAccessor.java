package io.wispforest.accessories.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.StateHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StateHolder.class)
public interface StateHolderAccessor<O, S> {
    @Accessor("owner")
    O accessories$owner();
    
    @Accessor("propertiesCodec")
    MapCodec<S> accessories$propertiesCodec();
}
