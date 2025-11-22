package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.data.AccessoriesTags;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.core.AccessoryNestUtils;
import io.wispforest.accessories.pond.AccessoriesLivingEntityExtension;
import io.wispforest.accessories.pond.EnchantedItemInUseExtension;
import io.wispforest.accessories.utils.ServerInstanceHolder;
import io.wispforest.owo.Owo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Predicate;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Invoker("runIterationOnItem")
    public static void accessoriess$runIterationOnItem(ItemStack itemStack, EquipmentSlot equipmentSlot, LivingEntity livingEntity, EnchantmentHelper.EnchantmentInSlotVisitor enchantmentInSlotVisitor) {}

    @WrapOperation(method = "getEnchantmentLevel", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private static Collection<ItemStack> addAccessoriesStacks(Map instance, Operation<Collection<ItemStack>> original, @Local(argsOnly = true) Holder<Enchantment> enchantment, @Local(argsOnly = true) LivingEntity entity){
        var returnValue = new ArrayList<>(original.call(instance));

        //if(Accessories.enchantmentValidForRedirect(enchantment)) {
        var capability = entity.accessoriesCapability();

        if(capability != null) {
            returnValue.addAll(capability.getAllEquipped().stream().map(SlotEntryReference::stack).toList());
        }
        //}

        return returnValue;
    }

//    @ModifyReturnValue(method = "getEnchantmentLevel", at = @At(value = "RETURN"))
//    private static int adjustEnchantmentLevel(int original, @Local(argsOnly = true) LivingEntity livingEntity, @Local(argsOnly = true) Holder<Enchantment> holder){
//        var enchantments = livingEntity.registryAccess().registry(Registries.ENCHANTMENT).orElseThrow();
//
//        if(enchantments.getResourceKey(holder.value()).orElseThrow().equals(Enchantments.LOOTING)){
//            ExtraEventHandler.lootingAdjustments(livingEntity, , value)
//        }
//
//        return original;
//    }

    @Inject(method = "getRandomItemWith", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getRandom()Lnet/minecraft/util/RandomSource;"))
    private static void adjustListForAccessories(DataComponentType<?> dataComponentType, LivingEntity livingEntity, Predicate<ItemStack> predicate, CallbackInfoReturnable<Optional<EnchantedItemInUse>> cir, @Local(ordinal = 0) List<EnchantedItemInUse> list) {
        var capability = livingEntity.accessoriesCapability();

        if(capability != null){
            var allEquippedAccessories = capability
                    .getAllEquipped()
                    .stream()
                    .filter(entryReference -> {
                        var itemStack = entryReference.stack();

                        if(predicate.test(entryReference.stack())) {
                            ItemEnchantments itemEnchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

                            for(var entry : itemEnchantments.entrySet()) {
                                var holder = entry.getKey();

                                if (holder.value().effects().has(dataComponentType)) { //((Enchantment)holder.value()).matchingSlot(equipmentSlot)
                                    var valid = enchantmentValidForRedirect(livingEntity.registryAccess(), holder.value());

                                    if(valid != null) return valid;
                                }
                            }
                        }

                        return false;
                    }).map(entryReference -> {
                        return ((EnchantedItemInUseExtension) (Object) new EnchantedItemInUse(entryReference.stack(), AccessoriesInternals.INSTANCE.getInternalEquipmentSlot(), livingEntity, item -> entryReference.reference().breakStack()))
                                .setSlotReference(entryReference.reference());
                    })
                    .toList();

            list.addAll(allEquippedAccessories);
        }
    }

    @Inject(method = "runIterationOnEquipment", at = @At("TAIL"))
    private static void adjustIterationWithAccessories(LivingEntity livingEntity, EnchantmentHelper.EnchantmentInSlotVisitor enchantmentInSlotVisitor, CallbackInfo ci) {
        var capability = livingEntity.accessoriesCapability();

        if(capability != null){
            capability.getAllEquipped()
                    .forEach(entryReference -> {
                        var itemStack = entryReference.stack();

                        ((AccessoriesLivingEntityExtension) livingEntity).pushEnchantmentContext(itemStack, entryReference.reference());
                        accessoriess$runIterationOnItem(itemStack, AccessoriesInternals.INSTANCE.getInternalEquipmentSlot(), livingEntity, enchantmentInSlotVisitor);
                    });
        }
    }

    @WrapMethod(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V")
    private static void unpackAccessoryNest1(ItemStack stack, EnchantmentHelper.EnchantmentVisitor visitor, Operation<Void> original) {
        AccessoryNestUtils.recursivelyConsume(stack, innerStack -> original.call(innerStack, visitor));
    }

    @WrapMethod(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V")
    private static void unpackAccessoryNest2(ItemStack stack, EquipmentSlot slot, LivingEntity entity, EnchantmentHelper.EnchantmentInSlotVisitor visitor, Operation<Void> original) {
        AccessoryNestUtils.recursivelyConsume(stack, innerStack -> original.call(stack, slot, entity, visitor));
    }

    @ModifyExpressionValue(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;matchingSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z")
    )
    private static boolean adjustIfIterationOccurs(boolean original, @Local(argsOnly = true) EquipmentSlot equipmentSlot, @Local(argsOnly = true) LivingEntity livingEntity, @Local(ordinal = 0) Holder<Enchantment> holder) {
        if(equipmentSlot.equals(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) {
            var valid = enchantmentValidForRedirect(livingEntity.registryAccess(), holder.value());

            if(valid != null) return valid;
        }

        return original;
    }

    @WrapOperation(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At(value = "NEW", target = "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/enchantment/EnchantedItemInUse;")
    )
    private static EnchantedItemInUse addSlotReferenceToEnchantRecord(ItemStack itemStack, EquipmentSlot inSlot, LivingEntity owner, Operation<EnchantedItemInUse> original) {
        EnchantedItemInUse record = null;

        if (inSlot.equals(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) {
            var ref = ((AccessoriesLivingEntityExtension) owner).popEnchantmentContext(itemStack);

            if (ref != null) {
                record = new EnchantedItemInUse(itemStack, inSlot, owner, item -> ref.breakStack());

                ((EnchantedItemInUseExtension)(Object) record).setSlotReference(ref);
            }
        }

        if (record == null) record = original.call(itemStack, inSlot, owner);

        return record;
    }

    @WrapOperation(method = "method_60148", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;matchingSlot(Lnet/minecraft/world/entity/EquipmentSlot;)Z"))
    private static boolean allowAccessoriesSlotEnchentments(Enchantment instance, EquipmentSlot slot, Operation<Boolean> original) {
        if (slot.equals(AccessoriesInternals.INSTANCE.getInternalEquipmentSlot())) {
            var valid = enchantmentValidForRedirect(null, instance);

            if(valid != null) return valid;
        }

        return original.call(instance, slot);
    }

    @Unique
    @Nullable
    private static Boolean enchantmentValidForRedirect(@Nullable RegistryAccess access, Enchantment enchantment) {
        Registry<Enchantment> enchantments;

        if (access != null) {
            enchantments = access.lookupOrThrow(Registries.ENCHANTMENT);
        } else {
            // THIS IS VERY CRING BUT LACKING CONTEXT MEANS NOT MUCH CAN BE DONE
            var server = ServerInstanceHolder.getInstance();

            if (server == null) return null;

            enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        }

        return !enchantments.get(enchantments.getResourceKey(enchantment).orElseThrow())
                .orElseThrow()
                .is(AccessoriesTags.INVALID_FOR_REDIRECTION);
    }
}
