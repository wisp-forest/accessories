package io.wispforest.accessories.mixin.client.owo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.client.gui.components.ComponentUtils;
import io.wispforest.owo.ui.base.BaseParentComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.PositionedRectangle;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BaseParentComponent.class, remap = false)
public abstract class BaseParentComponentMixin {

    //--

    @WrapOperation(method = "drawChildren", at = @At(value = "INVOKE", target = "Lio/wispforest/owo/ui/core/OwoUIDrawContext;intersectsScissor(Lio/wispforest/owo/ui/core/PositionedRectangle;)Z"))
    private boolean disableSlotsNotDrawn(OwoUIDrawContext instance, PositionedRectangle other, Operation<Boolean> original) {
        var result = original.call(instance, other);

        if (!result) {
            if (other instanceof AccessoriesScreen.ExtendedSlotComponent slotComponent) {
                ((OwoSlotExtension) slotComponent.slot()).owo$setDisabledOverride(true);
            } else if (other instanceof ParentComponent parentComponent) {
                ComponentUtils.recursiveSearch(parentComponent, AccessoriesScreen.ExtendedSlotComponent.class, slotComponent -> {
                    ((OwoSlotExtension) slotComponent.slot()).owo$setDisabledOverride(true);
                });
            }
        }

        return result;
    }
}
