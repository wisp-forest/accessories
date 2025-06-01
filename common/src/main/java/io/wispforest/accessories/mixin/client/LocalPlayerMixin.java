package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.pond.CloseContainerTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements CloseContainerTransfer {

    @Unique
    private Screen transferedScreen = null;

    @WrapOperation(method = "clientSideCloseContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void test(Minecraft instance, Screen screen, Operation<Void> original) {
        original.call(instance, transferedScreen != null ? transferedScreen : screen);

        this.transferedScreen = null;
    }

    @Override
    public void accessories$setScreenTransfer(Screen screen) {
        this.transferedScreen = screen;
    }
}
