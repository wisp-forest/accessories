package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.pond.AccessoriesLivingEntityExtension;
import io.wispforest.accessories.pond.EnchantedItemInUseExtension;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "runLocationChangedEffects", at = @At("HEAD"), cancellable = true)
    private void failSafeForInvalidRecordObjects1(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, LivingEntity entity, CallbackInfo ci) {
        if(item.inSlot() == AccessoriesInternals.INSTANCE.getInternalEquipmentSlot()) {
            var ref = ((EnchantedItemInUseExtension) (Object) item).getSlotReference();

            if (ref == null) ci.cancel();
        }
    }

    @Inject(method = "stopLocationBasedEffects", at = @At("HEAD"), cancellable = true)
    private void failSafeForInvalidRecordObjects2(int enchantmentLevel, EnchantedItemInUse item, LivingEntity entity, CallbackInfo ci) {
        if(item.inSlot() == AccessoriesInternals.INSTANCE.getInternalEquipmentSlot()) {
            var ref = ((EnchantedItemInUseExtension) (Object) item).getSlotReference();

            if (ref == null) ci.cancel();
        }
    }

    @WrapOperation(method = "runLocationChangedEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;matchingSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z"))
    private boolean checkIfEnchantmentValid(Enchantment instance, EquipmentSlot slot, Operation<Boolean> original, @Local(argsOnly = true) ServerLevel level){
        return slot.equals(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())
                ? original.call(instance, slot)
                : enchantmentValidForRedirect(level.registryAccess(), instance);
    }

    @WrapOperation(method = {
            "runLocationChangedEffects",
            "stopLocationBasedEffects"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;activeLocationDependentEnchantments(Lnet/minecraft/world/entity/EquipmentSlot;)Ljava/util/Map;"))
    private Map<Enchantment, Set<EnchantmentLocationBasedEffect>> adjustMapLookupForRecord(LivingEntity instance, EquipmentSlot slot, Operation<Map<Enchantment, Set<EnchantmentLocationBasedEffect>>> original, @Local(argsOnly = true) EnchantedItemInUse item) {
        if (slot.equals(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) {
            var ref = ((EnchantedItemInUseExtension) (Object) item).getSlotReference();

            return (ref != null)
                    ? ((AccessoriesLivingEntityExtension) instance).activeLocationDependentEnchantmentsFromSlotReference(ref)
                    : new HashMap<>();
        }

        return original.call(instance, slot);
    }

    @Unique
    private static boolean enchantmentValidForRedirect(RegistryAccess access, Enchantment enchantment) {
        var enchantments = access.lookupOrThrow(Registries.ENCHANTMENT);

        return !enchantments.get(enchantments.getResourceKey(enchantment).orElseThrow())
                .orElseThrow()
                .is(AccessoriesTags.INVALID_FOR_REDIRECTION);
    }
}
