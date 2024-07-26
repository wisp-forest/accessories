package io.wispforest.accessories.neoforge.mixin;

import io.wispforest.accessories.client.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(NeoForgeLoadingOverlay.class)
public abstract class NeoForgeLoadingOverlayMixin {

    @Shadow @Final private Minecraft minecraft;

    @ModifyVariable(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/resources/ReloadInstance;Ljava/util/function/Consumer;Z)V", shift = At.Shift.AFTER), argsOnly = true)
    private Consumer<Optional<Throwable>> addEventHook(Consumer<Optional<Throwable>> value) {
        return (throwable) -> {
            ClientLifecycleEvents.END_DATA_PACK_RELOAD.invoker().endDataPackReload(this.minecraft, throwable.isEmpty());

            value.accept(throwable);
        };
    }
}
