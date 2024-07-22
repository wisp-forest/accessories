package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.accessories.client.gui.utils.AbstractPolygon;
import io.wispforest.accessories.pond.owo.ExclusiveBoundingArea;
import io.wispforest.accessories.pond.owo.InclusiveBoundingArea;
import io.wispforest.accessories.pond.owo.MutableBoundingArea;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.ParentComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Component.class, remap = false)
public interface ComponentMixin {

    @Inject(method = "isInBoundingBox", at = @At("HEAD"), cancellable = true)
    private void checkIfInExclusionZones(double x, double y, CallbackInfoReturnable<Boolean> cir) {
        var value = recursiveBoundsCheck((Component) (Object) this, x, y);
        if(value != null) cir.setReturnValue(value);
    }

    @Unique
    @Nullable
    private Boolean recursiveBoundsCheck(Component component, double x, double y) {
        if(component instanceof ExclusiveBoundingArea area && !area.getExclusionZones().isEmpty() && area.isWithinExclusionZone((float) x, (float) y)) {
            return false;
        }

        if(component instanceof InclusiveBoundingArea area && !area.getInclusionZones().isEmpty() && area.isWithinInclusionZone((float) x, (float) y)) {
            return true;
        }

        if(component instanceof ParentComponent childParent && component instanceof MutableBoundingArea area && area.deepRecursiveChecking()) {
            for (Component child : childParent.children()) {
                var value = recursiveBoundsCheck(child, x, y);

                if(value != null) return value;
            }
        }

        return null;
    }
}