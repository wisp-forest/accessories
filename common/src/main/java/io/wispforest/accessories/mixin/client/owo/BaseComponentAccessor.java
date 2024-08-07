package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.owo.ui.base.BaseComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseComponent.class)
public interface BaseComponentAccessor {
    @Accessor("mounted") boolean accessories$IsMounted();
}
