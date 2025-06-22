package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.impl.event.AccessoryEquipmentDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
    @WrapMethod(method = "getDefaultDispenseMethod")
    private static DispenseItemBehavior accessories$wrapWithAccessoryEquip(ItemStack stack, Operation<DispenseItemBehavior> original) {
        return new AccessoryEquipmentDispenseItemBehavior(original.call(stack));
    }
}
