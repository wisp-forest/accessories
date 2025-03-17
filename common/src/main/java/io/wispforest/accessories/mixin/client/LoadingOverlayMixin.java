package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.client.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @ModifyVariable(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Overlay;<init>()V", shift = At.Shift.AFTER), argsOnly = true)
    private Consumer<Optional<Throwable>> addEventHook(Consumer<Optional<Throwable>> value) {
        return (throwable) -> {
            ClientLifecycleEvents.END_DATA_PACK_RELOAD.invoker().endDataPackReload(Minecraft.getInstance(), throwable.isEmpty());

            value.accept(throwable);
        };
    }
}
