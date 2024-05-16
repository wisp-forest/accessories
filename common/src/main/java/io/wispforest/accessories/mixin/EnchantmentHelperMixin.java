package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @WrapOperation(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private static Collection<ItemStack> addAccessoriesStacks(Map instance, Operation<Collection<ItemStack>> original, @Local(argsOnly = true) Enchantment enchantment, @Local(argsOnly = true) LivingEntity entity){
        var returnValue = new ArrayList<>(original.call(instance));

        //if(Accessories.enchantmentValidForRedirect(enchantment)) {
        var capability = entity.accessoriesCapability();

        if(capability != null) {
            returnValue.addAll(capability.getAllEquipped().stream().map(SlotEntryReference::stack).toList());
        }
        //}

        return returnValue;
    }

    @WrapOperation(method = "getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;", at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z"))
    private static boolean wrapMapCheck(Map instance, Operation<Boolean> original, Enchantment enchantment, LivingEntity livingEntity, Predicate<ItemStack> stackCondition, @Share("slotEntries") LocalRef<List<SlotEntryReference>> slotEntries) {
        var bl = original.call(instance);

        if(Accessories.enchantmentValidForRedirect(enchantment) && livingEntity.accessoriesCapability() != null){
            var allEquippedAccessories = livingEntity.accessoriesCapability().getAllEquipped().stream()
                    .filter(entryReference -> {
                        var stack = entryReference.stack();

                        var level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);

                        return level > 0 && stackCondition.test(stack);
                    }).toList();

            slotEntries.set(allEquippedAccessories);

            bl = bl && allEquippedAccessories.isEmpty();
        } else {
            slotEntries.set(List.of());
        }

        return bl;
    }

    @Inject(method = "getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", shift = At.Shift.BEFORE), cancellable = true)
    private static void attemptRedirectToAccessories(Enchantment enchantment, LivingEntity livingEntity, Predicate<ItemStack> stackCondition, CallbackInfoReturnable<Map.@Nullable Entry<EquipmentSlot, ItemStack>> cir, @Local(ordinal = 0) List<Map.Entry<EquipmentSlot, ItemStack>> list, @Share("slotEntries") LocalRef<List<SlotEntryReference>> slotEntries) {
        if(!Accessories.enchantmentValidForRedirect(enchantment)) return;

        var allEquippedAccessories = slotEntries.get();

        if(allEquippedAccessories.isEmpty()) return;

        var selectedRef = allEquippedAccessories.get(livingEntity.getRandom().nextInt(allEquippedAccessories.size()));

        list.add(Map.entry(Accessories.getInternalSlot(), selectedRef.stack()));
    }
}
