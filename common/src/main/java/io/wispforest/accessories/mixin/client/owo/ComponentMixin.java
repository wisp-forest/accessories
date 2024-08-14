package io.wispforest.accessories.mixin.client.owo;

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

    @Inject(method = "isInBoundingBox", at = @At("HEAD"), cancellable = true, remap = false)
    private void checkBoundingAreaZones(double x, double y, CallbackInfoReturnable<Boolean> cir) {
        var value = recursiveBoundsCheck((Component) (Object) this, x, y);
        if(value != null) cir.setReturnValue(value);
    }

    @Unique
    @Nullable
    private Boolean recursiveBoundsCheck(Component component, double x, double y) {
        if(component instanceof ParentComponent parent) {
            if (parent instanceof ExclusiveBoundingArea area && area.isWithinExclusionZone((float) x, (float) y)) {
                return false;
            }

            if (parent instanceof InclusiveBoundingArea area && area.isWithinInclusionZone((float) x, (float) y)) {
                return true;
            }

            if (parent instanceof MutableBoundingArea area && area.deepRecursiveChecking()){
                for (var child : parent.children()) {
                    var value = recursiveBoundsCheck(child, x, y);

                    if (value != null) return value;
                }
            }
        }

        return null;
    }
}
