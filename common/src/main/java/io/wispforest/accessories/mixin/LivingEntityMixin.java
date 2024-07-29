package io.wispforest.accessories.mixin;

import com.google.common.collect.Iterables;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements AccessoriesAPIAccess {

    @Override
    @Nullable
    public AccessoriesCapability accessoriesCapability() {
        var slots = EntitySlotLoader.getEntitySlots((LivingEntity) (Object) this);

        if(slots.isEmpty()) return null;

        return new AccessoriesCapabilityImpl((LivingEntity) (Object) this);
    }

    @Override
    @Nullable
    public AccessoriesHolder accessoriesHolder() {
        var capability = accessoriesCapability();

        return capability != null ? capability.getHolder() : null;
    }

    //--

    @Inject(method = "onEquippedItemBroken", at = @At("HEAD"), cancellable = true)
    private void sendAccessoriesBreakInstead(Item item, EquipmentSlot slot, CallbackInfo ci){
        if(slot.equals(AccessoriesInternals.INTERNAL_SLOT)) ci.cancel();
    }

    @Inject(method = "entityEventForEquipmentBreak", at = @At("HEAD"), cancellable = true)
    private static void preventMatchExceptionForAccessories(EquipmentSlot slot, CallbackInfoReturnable<Byte> cir) {
        if(slot.equals(AccessoriesInternals.INTERNAL_SLOT)) cir.setReturnValue((byte) -1);
    }

//    @WrapOperation(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getArmorAndBodyArmorSlots()Ljava/lang/Iterable;"))
//    private Iterable<ItemStack> addAccessories(LivingEntity instance, Operation<Iterable<ItemStack>> original){
//        var iterable = original.call(instance);
//
//        if((Object) this instanceof LivingEntity livingEntity) {
//            var capability = livingEntity.accessoriesCapability();
//
//            if(capability != null) iterable = Iterables.concat(iterable, capability.getAllEquipped().stream().map(SlotEntryReference::stack).toList());
//        }
//
//        return iterable;
//    }

    // TODO: SEEM IF THIS IS STILL SOMETHING TO HAVE WITHIN THE FUTURE! BUGS OCCUR THOUGH WITH INV HUD+
//    @ModifyReturnValue(method = "getAllSlots", at = @At("RETURN"))
//    private Iterable<ItemStack> addAccessories(Iterable<ItemStack> original){
//        if((Object) this instanceof LivingEntity livingEntity && !livingEntity.isRemoved()) {
//            var capability = livingEntity.accessoriesCapability();
//
//            if(capability != null) return Iterables.concat(original, capability.getAllEquipped().stream().map(SlotEntryReference::stack).toList());
//        }
//
//        return original;
//    }
}
