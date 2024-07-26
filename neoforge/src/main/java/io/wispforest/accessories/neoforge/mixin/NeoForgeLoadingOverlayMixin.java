package io.wispforest.accessories.neoforge.mixin;

import io.wispforest.accessories.client.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = NeoForgeLoadingOverlay.class, remap = false)
public abstract class NeoForgeLoadingOverlayMixin {

    @ModifyVariable(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/resources/ReloadInstance;Ljava/util/function/Consumer;Z)V", shift = At.Shift.AFTER),
            argsOnly = true, remap = false)
    private Consumer<Optional<Throwable>> addEventHook(Consumer<Optional<Throwable>> value) {
        return (throwable) -> {
            ClientLifecycleEvents.END_DATA_PACK_RELOAD.invoker().endDataPackReload(Minecraft.getInstance(), throwable.isEmpty());

            value.accept(throwable);
        };
    }
}
