package io.wispforest.accessories.mixin.client.owo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.client.gui.components.ComponentUtils;
import io.wispforest.accessories.client.gui.utils.AbstractPolygon;
import io.wispforest.accessories.pond.owo.MutableBoundingArea;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.base.BaseParentComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.util.pond.OwoSlotExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BaseParentComponent.class, remap = false)
public abstract class BaseParentComponentMixin implements MutableBoundingArea<BaseParentComponent> {

    private boolean deepRecursiveChecking = false;

    @Override
    public BaseParentComponent deepRecursiveChecking(boolean value) {
        this.deepRecursiveChecking = value;

        return (BaseParentComponent) (Object) this;
    }

    @Override
    public boolean deepRecursiveChecking() {
        return this.deepRecursiveChecking;
    }

    //--

    @Unique
    private final List<AbstractPolygon> inclusionZones = new ArrayList<>();

    @Override
    public List<AbstractPolygon> getInclusionZones() {
        return inclusionZones;
    }

    @SafeVarargs
    @Override
    public final <P extends AbstractPolygon> BaseParentComponent addInclusionZone(P... polygons) {
        this.inclusionZones.addAll(List.of(polygons));

        return (BaseParentComponent) (Object) this;
    }

    @Override
    public final <P extends AbstractPolygon> BaseParentComponent addInclusionZone(List<P> polygons) {
        this.inclusionZones.addAll(polygons);

        return (BaseParentComponent) (Object) this;
    }

    //--

    @Unique
    private final List<AbstractPolygon> exclusionZones = new ArrayList<>();

    @Override
    public List<AbstractPolygon> getExclusionZones() {
        return exclusionZones;
    }

    @SafeVarargs
    @Override
    public final <P extends AbstractPolygon> BaseParentComponent addExclusionZone(P... polygons) {
        this.exclusionZones.addAll(List.of(polygons));

        return (BaseParentComponent) (Object) this;
    }

    @Override
    public final <P extends AbstractPolygon> BaseParentComponent addExclusionZone(List<P> polygons) {
        this.exclusionZones.addAll(polygons);

        return (BaseParentComponent) (Object) this;
    }

    //--

    @WrapOperation(method = "drawChildren", at = @At(value = "INVOKE", target = "Lio/wispforest/owo/ui/util/ScissorStack;isVisible(Lio/wispforest/owo/ui/core/Component;Lcom/mojang/blaze3d/vertex/PoseStack;)Z"))
    private boolean disableSlotsNotDrawn(Component component, PoseStack matrices, Operation<Boolean> original) {
        var result = original.call(component, matrices);



        if (!result) {
            if (component instanceof AccessoriesExperimentalScreen.ExtendedSlotComponent slotComponent) {
                ((OwoSlotExtension) slotComponent.slot()).owo$setDisabledOverride(true);
            } else if (component instanceof ParentComponent parentComponent) {
                ComponentUtils.recursiveSearch(parentComponent,  AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponent -> {
                    ((OwoSlotExtension) slotComponent.slot()).owo$setDisabledOverride(true);
                });
            }
        }

        return result;
    }
}
