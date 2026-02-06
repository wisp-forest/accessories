package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Predicate;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Shadow
    private static void runIterationOnItem(ItemStack itemStack, EquipmentSlot equipmentSlot, LivingEntity livingEntity, EnchantmentHelper.EnchantmentInSlotVisitor enchantmentInSlotVisitor) {}

    @WrapOperation(method = "getEnchantmentLevel", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private static Collection<ItemStack> addAccessoriesStacks(Map instance, Operation<Collection<ItemStack>> original, @Local(argsOnly = true) Holder<Enchantment> enchantment, @Local(argsOnly = true) LivingEntity entity){
        var stacks = original.call(instance);

        //if(Accessories.enchantmentValidForRedirect(enchantment)) {
        var capability = entity.accessoriesCapability();

        if(capability != null) {
            stacks = new ArrayList<>(stacks);

            for (var slotEntryReference : capability.getAllEquipped()) {
                stacks.add(slotEntryReference.stack());
            }
        }
        //}

        return stacks;
    }

    @Inject(method = "getRandomItemWith", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getRandom()Lnet/minecraft/util/RandomSource;"))
    private static void adjustListForAccessories(DataComponentType<?> dataComponentType, LivingEntity livingEntity, Predicate<ItemStack> predicate, CallbackInfoReturnable<Optional<EnchantedItemInUse>> cir, @Local(ordinal = 0) List<EnchantedItemInUse> list) {
        var capability = livingEntity.accessoriesCapability();

        if(capability != null){
            for (var ref : capability.getAllEquipped()) {
                var itemStack = ref.stack();

                if(!predicate.test(ref.stack())) continue;

                var itemEnchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

                for(var entry : itemEnchantments.entrySet()) {
                    var holder = entry.getKey();
                    var value = holder.value();

                    if (value.effects().has(dataComponentType) && enchantmentValidForRedirect(livingEntity.registryAccess(), value)) { //((Enchantment)holder.value()).matchingSlot(equipmentSlot)
                        list.add(new EnchantedItemInUse(itemStack, AccessoriesInternals.INTERNAL_SLOT, livingEntity, item -> AccessoriesAPI.breakStack(ref.reference())));
                    }
                }
            }
        }
    }

    @Inject(method = "runIterationOnEquipment", at = @At("TAIL"))
    private static void adjustIterationWithAccessories(LivingEntity livingEntity, EnchantmentHelper.EnchantmentInSlotVisitor enchantmentInSlotVisitor, CallbackInfo ci) {
        var capability = livingEntity.accessoriesCapability();

        if(capability != null){
            for (var ref : capability.getAllEquipped()) {
                runIterationOnItem(ref.stack(), AccessoriesInternals.INTERNAL_SLOT, livingEntity, enchantmentInSlotVisitor);
            }
        }
    }

    @ModifyExpressionValue(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;matchingSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z")
    )
    private static boolean adjustIfIterationOccurs(boolean original, @Local(argsOnly = true) EquipmentSlot equipmentSlot, @Local(argsOnly = true) LivingEntity livingEntity, @Local(ordinal = 0) Holder<Enchantment> holder) {
        if(equipmentSlot.equals(AccessoriesInternals.INTERNAL_SLOT) && enchantmentValidForRedirect(livingEntity.registryAccess(), holder.value())) {
            return true;
        }

        return original;
    }

    @Unique
    private static boolean enchantmentValidForRedirect(RegistryAccess access, Enchantment enchantment) {
        var enchantments = access.registry(Registries.ENCHANTMENT).orElseThrow();

        return !enchantments.getHolder(enchantments.getResourceKey(enchantment).orElseThrow())
                .orElseThrow()
                .is(AccessoriesTags.INVALID_FOR_REDIRECTION);
    }
}
