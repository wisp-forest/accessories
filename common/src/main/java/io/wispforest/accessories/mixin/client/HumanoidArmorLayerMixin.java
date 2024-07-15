package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.menu.ArmorSlotTypes;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack adjustForCosmeticArmor(LivingEntity instance, EquipmentSlot equipmentSlot, Operation<ItemStack> original) {
        var capability = instance.accessoriesCapability();

        if(capability != null) {
            var reference = ArmorSlotTypes.getReferenceFromSlot(equipmentSlot);

            if(reference != null) {
                var container = capability.getContainer(reference);

                if(container != null) {
                    var stack = container.getCosmeticAccessories().getItem(0);

                    if(!stack.isEmpty()) return stack;
                }
            }
        }

        return original.call(instance, equipmentSlot);
    }
}
