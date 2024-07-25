//package io.wispforest.accessories.mixin;
//
//import com.google.common.collect.HashMultimap;
//import io.wispforest.accessories.Accessories;
//import io.wispforest.accessories.AccessoriesInternals;
//import net.minecraft.core.Holder;
//import net.minecraft.world.entity.EquipmentSlot;
//import net.minecraft.world.entity.ai.attributes.Attribute;
//import net.minecraft.world.entity.ai.attributes.AttributeModifier;
//import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(EnchantmentAttributeEffect.class)
//public abstract class EnchantmentAttributeEffectMixin {
//
//    @Inject(method = "makeAttributeMap", at = @At("HEAD"), cancellable = true)
//    private void returnEmptyIfAccessoriesSlot(int i, EquipmentSlot equipmentSlot, CallbackInfoReturnable<HashMultimap<Holder<Attribute>, AttributeModifier>> cir) {
//        if(equipmentSlot.equals(AccessoriesInternals.INTERNAL_SLOT)) cir.setReturnValue(HashMultimap.create());
//    }
//}
