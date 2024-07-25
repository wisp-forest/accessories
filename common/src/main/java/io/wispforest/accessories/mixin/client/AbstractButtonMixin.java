package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.client.gui.AbstractButtonExtension;
import io.wispforest.accessories.client.gui.ButtonEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin implements AbstractButtonExtension {

    @Unique
    private final Event<ButtonEvents.AdjustRendering> ADJUST_RENDERING_EVENT = EventFactory.createArrayBacked(ButtonEvents.AdjustRendering.class, invokers -> (button, instance, sprite, x, y, width, height) -> {
        boolean shouldCancel = false;

        for (var invoker : invokers) shouldCancel = invoker.render(button, instance, sprite, x, y, width, height);

        return shouldCancel;
    });

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIII)V"))
    private void adjustButtonRendering(GuiGraphics instance, ResourceLocation resourceLocation, int x, int y, int width, int height, int m, int n, int o, int p, int q, int r, Operation<Void> original) {
        boolean value = ADJUST_RENDERING_EVENT.invoker().render((AbstractButton) (Object) this, instance, resourceLocation, x, y, width, height);

        if(!value) original.call(instance, resourceLocation, x, y, width, height, m, n, o, p, q, r);
    }


    @Override
    public Event<ButtonEvents.AdjustRendering> getRenderingEvent() {
        return ADJUST_RENDERING_EVENT;
    }
}
