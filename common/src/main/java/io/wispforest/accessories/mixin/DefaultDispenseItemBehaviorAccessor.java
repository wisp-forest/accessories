package io.wispforest.accessories.mixin;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DefaultDispenseItemBehavior.class)
public interface DefaultDispenseItemBehaviorAccessor {
    @Invoker("execute")
    ItemStack accessories$execute(BlockSource blockSource, ItemStack item);
}
