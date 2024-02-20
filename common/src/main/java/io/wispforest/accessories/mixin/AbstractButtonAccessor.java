package io.wispforest.accessories.mixin;

import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractButton.class)
public interface AbstractButtonAccessor {
    @Invoker("getTextureY")
    int accessories$getTextureY();
}
