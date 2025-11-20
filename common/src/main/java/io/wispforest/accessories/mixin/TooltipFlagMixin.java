package io.wispforest.accessories.mixin;

import io.wispforest.accessories.pond.TooltipFlagExtension;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TooltipFlag.class)
public interface TooltipFlagMixin extends TooltipFlagExtension {

}
