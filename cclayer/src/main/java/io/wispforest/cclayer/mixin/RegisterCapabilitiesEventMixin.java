package io.wispforest.cclayer.mixin;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.cclayer.CCLayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.compat.WrappedICurioProvider;

import java.util.Objects;

@Mixin(RegisterCapabilitiesEvent.class)
public abstract class RegisterCapabilitiesEventMixin {
    @Inject(method = "registerItem", at = @At("HEAD"))
    private <T, C> void hookForBypassingCuriosRegisterCall(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, ItemLike[] items, CallbackInfo ci) {
        if(capability.equals(CuriosCapability.ITEM) && !provider.equals(CCLayer.BASE_PROVIDER)){
            ICapabilityProvider<ItemStack, Void, ICurio> icurioProvider = (ICapabilityProvider<ItemStack, Void, ICurio>) (Object) provider;

            var wrappedCurio = new WrappedICurioProvider(icurioProvider);

            for (var itemLike : items) AccessoriesAPI.registerAccessory(Objects.requireNonNull(itemLike.asItem()), wrappedCurio);
        }
    }
}
