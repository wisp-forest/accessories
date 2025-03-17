package io.wispforest.accessories.mixin.client;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.accessories.client.AccessoriesClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Final private Window window;

    @Inject(method = "resizeDisplay", at = @At(value = "TAIL"))
    private void captureResize(CallbackInfo ci){
        AccessoriesClient.WINDOW_RESIZE_CALLBACK_EVENT.invoker().onResized(((Minecraft) ((Object) this)), this.window);
    }

    @Unique
    private static final ResourceLocation SPRITE_ATLAS_LOCATION = ResourceLocation.withDefaultNamespace("textures/atlas/gui.png");

    @Inject(method = "getTextureAtlas", at = @At(value = "HEAD"), cancellable = true)
    private void allowForGuiSprites(ResourceLocation location, CallbackInfoReturnable<Function<ResourceLocation, TextureAtlasSprite>> cir) {
        if(location.equals(SPRITE_ATLAS_LOCATION)) {
            cir.setReturnValue(Minecraft.getInstance().getGuiSprites()::getSprite);
        }
    }
}