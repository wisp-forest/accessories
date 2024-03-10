package io.wispforest.tclayer.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.data.SlotLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io/wispforest/accessories/fabric/AccessoriesFabric")
public abstract class AccessoriesFabricMixin {

    @Inject(method = "onInitialize", at = @At("TAIL"))
    private void adjustDependenciesAndAddListeners(CallbackInfo ci, @Local ResourceManagerHelper manager, @Local(ordinal = 0) IdentifiableResourceReloadListener slotTypeLoader, @Local(ordinal = 1) IdentifiableResourceReloadListener entitySlotLoader){
        manager.registerReloadListener(SlotLoader.INSTANCE);
        manager.registerReloadListener(dev.emi.trinkets.data.EntitySlotLoader.SERVER);

        slotTypeLoader.getFabricDependencies().add(SlotLoader.INSTANCE.getFabricId());
        entitySlotLoader.getFabricDependencies().add(dev.emi.trinkets.data.EntitySlotLoader.SERVER.getFabricId());
    }
}
