package io.wispforest.accessories.mixin.client.owo;

import io.wispforest.accessories.client.gui.utils.AbstractPolygon;
import io.wispforest.accessories.pond.owo.ComponentExtension;
import io.wispforest.accessories.pond.owo.ExclusiveBoundingArea;
import io.wispforest.accessories.pond.owo.InclusiveBoundingArea;
import io.wispforest.owo.ui.base.BaseComponent;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(value = BaseComponent.class, remap = false)
public abstract class BaseComponentMixin implements ComponentExtension<BaseComponent> {

    @Shadow protected int x;
    @Shadow protected int y;

    @Inject(method = {"updateX", "updateY"}, at = @At("HEAD"))
    private void updateBoundingArea(int value, CallbackInfo ci){
        List<AbstractPolygon> polygons = new ArrayList<>();

        if(this instanceof InclusiveBoundingArea inclusiveBoundingArea){
            polygons.addAll(inclusiveBoundingArea.getInclusionZones());
        }

        if(this instanceof ExclusiveBoundingArea exclusiveBoundingArea){
            polygons.addAll(exclusiveBoundingArea.getExclusionZones());
        }

        if(polygons.isEmpty()) return;

        boolean isUpdateX = Objects.equals(ci.getId(), "updateX");

        int diff = value - (isUpdateX ? this.x : this.y);

        if(diff != 0) {
            Vector3f vec3f = new Vector3f(
                    isUpdateX ? diff : 0,
                    isUpdateX ? 0 : diff,
                    0f
            );

            polygons.forEach(abstractPolygon -> abstractPolygon.movePolygon(vec3f, Vector3f::add));
        }
    }

    //--

    @Unique
    private boolean accessories$allowIndividualOverdraw = false;

    @Override
    public BaseComponent allowIndividualOverdraw(boolean value) {
        this.accessories$allowIndividualOverdraw = value;

        return (BaseComponent) (Object) this;
    }

    @Override
    public boolean allowIndividualOverdraw() {
        return accessories$allowIndividualOverdraw;
    }
}
