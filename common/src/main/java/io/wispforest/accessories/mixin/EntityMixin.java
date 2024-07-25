package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


import com.google.common.collect.Iterables;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyReturnValue(method = "getAllSlots", at = @At("RETURN"))
    private Iterable<ItemStack> addAccessories(Iterable<ItemStack> original){
        if((Object) this instanceof LivingEntity livingEntity && !livingEntity.isRemoved()) {
            var capability = livingEntity.accessoriesCapability();

            if(capability != null) return Iterables.concat(original, capability.getAllEquipped().stream().map(SlotEntryReference::stack).toList());
        }

        return original;
    }
}
