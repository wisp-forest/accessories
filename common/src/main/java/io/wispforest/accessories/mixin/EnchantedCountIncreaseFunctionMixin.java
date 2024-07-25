//package io.wispforest.accessories.mixin;
//
//import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//import com.llamalad7.mixinextras.sugar.Local;
//import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
//import net.minecraft.core.Holder;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.item.enchantment.Enchantment;
//import net.minecraft.world.item.enchantment.Enchantments;
//import net.minecraft.world.level.storage.loot.LootContext;
//import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
//import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//
//@Mixin(EnchantedCountIncreaseFunction.class)
//public abstract class EnchantedCountIncreaseFunctionMixin {
//
//    @WrapOperation(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I"))
//    private int attemptAdjustCountWithAccessoriesLooting(Holder<Enchantment> holder, LivingEntity livingEntity, Operation<Integer> original, @Local(argsOnly = true) LootContext context) {
//        var amount = original.call(holder, livingEntity);
//
//        var enchantments = livingEntity.registryAccess().registry(Registries.ENCHANTMENT).orElseThrow();
//
//        var damageSource = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
//
//        if(enchantments.getResourceKey(holder.value()).orElseThrow().equals(Enchantments.LOOTING) && damageSource != null){
//            amount = ExtraEventHandler.lootingAdjustments(livingEntity, damageSource, amount);
//        }
//
//        return amount;
//    }
//}
