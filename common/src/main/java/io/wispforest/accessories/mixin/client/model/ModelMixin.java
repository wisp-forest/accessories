package io.wispforest.accessories.mixin.client.model;

import io.wispforest.accessories.pond.ModelPartLoadingHelper;
import io.wispforest.accessories.pond.ModelRootAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(Model.class)
public abstract class ModelMixin implements ModelRootAccess {

    @Nullable
    @Unique
    private ModelPart accessories$rootPart = null;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void accessories$saveRootPart(Function renderType, CallbackInfo ci) {
        if (((Model)(Object) this) instanceof HierarchicalModel<?>) return;

        // For cases where Modder's bypass model loading and instantiate models outside such loading
        var modelSet = Minecraft.getInstance().getEntityModels();
        if (modelSet == null) return;

        this.accessories$rootPart = ((ModelPartLoadingHelper) modelSet).accessories$pollRoot();
    }

    @Override
    @Nullable
    public ModelPart accessories$rootPart() {
        if (((Model)(Object) this) instanceof HierarchicalModel<?> hierarchicalModel){
            return hierarchicalModel.root();
        }

        return this.accessories$rootPart;
    }
}
