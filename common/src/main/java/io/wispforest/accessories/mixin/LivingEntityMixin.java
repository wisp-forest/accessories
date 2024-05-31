package io.wispforest.accessories.mixin;

import com.google.common.collect.Iterables;
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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "broadcastBreakEvent(Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("HEAD"), cancellable = true)
    private void sendAccessoriesBreakInstead(EquipmentSlot slot, CallbackInfo ci){
        if(slot.equals(AccessoriesInternals.INTERNAL_SLOT)) ci.cancel();
    }

    @WrapOperation(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getArmorSlots()Ljava/lang/Iterable;"))
    private Iterable<ItemStack> addAccessories(LivingEntity instance, Operation<Iterable<ItemStack>> original){
        var iterable = original.call(instance);

        if((Object) this instanceof LivingEntity livingEntity) {
            var capability = livingEntity.accessoriesCapability();

            if(capability != null) iterable = Iterables.concat(iterable, capability.getAllEquipped().stream().map(SlotEntryReference::stack).toList());
        }

        return iterable;
    }
}
