package io.wispforest.cclayer.mixin;

import io.wispforest.accessories.data.SlotTypeLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SlotTypeLoader.SlotBuilder.class)
public interface SlotTypeLoaderBuilderAccessor {

    @Accessor("amount") Integer getAmount();
}
