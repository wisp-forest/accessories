package io.wispforest.accessories.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Debug(export = true)
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("breakItem") public void accessors$breakItem(ItemStack stack);
}
