package io.wispforest.cclayer.mixin;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.common.data.CuriosSlotManager;

import java.util.function.Consumer;

@Pseudo
@Mixin(targets = "io/wispforest/accessories/neoforge/AccessoriesForge")
public abstract class AccessoriesForgeMixin {

    @Inject(method = "intermediateRegisterListeners", at = @At("HEAD"))
    private void registerAdditionalResourceLoaders(Consumer<PreparableReloadListener> registrationMethod, CallbackInfo ci) {
        registrationMethod.accept(CuriosSlotManager.INSTANCE);
        registrationMethod.accept(CuriosEntityManager.INSTANCE);
    }
}
