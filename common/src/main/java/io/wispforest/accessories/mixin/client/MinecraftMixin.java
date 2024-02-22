package io.wispforest.accessories.mixin.client;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.accessories.client.AccessoriesClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final private Window window;

    @Inject(method = "resizeDisplay", at = @At(value = "TAIL"))
    private void captureResize(CallbackInfo ci){
        AccessoriesClient.WINDOW_RESIZE_CALLBACK_EVENT.invoker().onResized(((Minecraft) ((Object) this)), this.window);
    }
}