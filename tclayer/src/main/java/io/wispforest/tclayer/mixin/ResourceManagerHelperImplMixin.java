package io.wispforest.tclayer.mixin;

import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.Accessories;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl.class, remap = false)
public class ResourceManagerHelperImplMixin {
    @Inject(method = "registerReloadListener", at = @At("HEAD"))
    private void adjustDependencies(IdentifiableResourceReloadListener listener, CallbackInfo ci) {
        if(listener.getFabricId().equals(Accessories.of("slot_loader"))) {
            listener.getFabricDependencies().add(SlotLoader.INSTANCE.getFabricId());
        }

        if(listener.getFabricId().equals(Accessories.of("entity_slot_loader"))) {
            listener.getFabricDependencies().add(dev.emi.trinkets.data.EntitySlotLoader.SERVER.getFabricId());
        }
    }
}
