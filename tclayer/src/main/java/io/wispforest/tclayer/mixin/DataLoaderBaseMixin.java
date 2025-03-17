package io.wispforest.tclayer.mixin;

import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.DataLoaderBase;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Pseudo
@Mixin(targets = "io/wispforest/accessories/DataLoaderBase", remap = false)
public abstract class DataLoaderBaseMixin {

    @Shadow(remap = false)
    protected abstract Optional<PreparableReloadListener> getIdentifiedSlotLoader();

    @Shadow(remap = false)
    protected abstract Optional<PreparableReloadListener> getIdentifiedEntitySlotLoader();

    @Inject(method = "registerListeners", at = @At("HEAD"), cancellable = true)
    private void adjustDependenciesAndAddListeners(CallbackInfo ci){
        var manager = ResourceManagerHelper.get(PackType.SERVER_DATA);

        manager.registerReloadListener(SlotLoader.INSTANCE);
        manager.registerReloadListener(dev.emi.trinkets.data.EntitySlotLoader.SERVER);

        getIdentifiedSlotLoader()
                .ifPresentOrElse(listener -> {
                    ((IdentifiableResourceReloadListener) listener).getFabricDependencies().add(SlotLoader.INSTANCE.getFabricId());
                }, () -> {
                    throw new IllegalStateException("Unable to get the required SlotLoader listener for TCLayer to add as dependency!");
                });

        getIdentifiedEntitySlotLoader()
                .ifPresentOrElse(listener -> {
                    ((IdentifiableResourceReloadListener) listener).getFabricDependencies().add(dev.emi.trinkets.data.EntitySlotLoader.SERVER.getFabricId());
                }, () -> {
                    throw new IllegalStateException("Unable to get the required EntitySlotLoader listener for TCLayer to add as dependency!");
                });

        DataLoaderBase.LOGGER.info("Registered Trinkets Reloaded Listeners");

        ci.cancel();
    }
}
