package io.wispforest.tclayer.mixin;

import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.DataLoaderBase;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io/wispforest/accessories/DataLoaderBase", remap = false)
public abstract class DataLoaderBaseMixin {

    @Inject(method = "registerListeners", at = @At("HEAD"), cancellable = true)
    private void addListeners(CallbackInfo ci){
        var manager = ResourceManagerHelper.get(PackType.SERVER_DATA);

        manager.registerReloadListener(SlotLoader.INSTANCE);
        manager.registerReloadListener(dev.emi.trinkets.data.EntitySlotLoader.SERVER);

        DataLoaderBase.LOGGER.info("Registered Trinkets Reloaded Listeners");

        ci.cancel();
    }
}
