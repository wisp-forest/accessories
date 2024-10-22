package io.wispforest.accessories.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.client.gui.AbstractButtonExtension;
import io.wispforest.accessories.client.gui.ButtonEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin implements AbstractButtonExtension {

    @Unique
    private final Event<ButtonEvents.AdjustRendering> ADJUST_RENDERING_EVENT = EventFactory.createArrayBacked(ButtonEvents.AdjustRendering.class, invokers -> (button, instance, sprite, x, y, width, height) -> {
        boolean shouldCancel = false;

        for (var invoker : invokers) shouldCancel = invoker.render(button, instance, sprite, x, y, width, height);

        return shouldCancel;
    });

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIII)V"))
    private void adjustButtonRendering(GuiGraphics instance, Function<ResourceLocation, RenderType> function, ResourceLocation resourceLocation, int x, int y, int width, int height, int m, Operation<Void> original) {
        boolean value = ADJUST_RENDERING_EVENT.invoker().render((AbstractButton) (Object) this, instance, resourceLocation, x, y, width, height);

        if(!value){
            original.call(instance, function, resourceLocation, x, y, width, height, m);
        }
    }

    @Override
    public Event<ButtonEvents.AdjustRendering> getRenderingEvent() {
        return ADJUST_RENDERING_EVENT;
    }
}
