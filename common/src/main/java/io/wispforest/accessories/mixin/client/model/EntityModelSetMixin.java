package io.wispforest.accessories.mixin.client.model;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.accessories.pond.ModelPartLoadingHelper;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(EntityModelSet.class)
public abstract class EntityModelSetMixin implements ModelPartLoadingHelper {

    @Unique
    private final Deque<ModelPart> accessories$modelPartStorage = new ArrayDeque<>();

    @Override
    public void accessories$pushRoot(ModelPart root) {
        accessories$modelPartStorage.push(root);
    }

    @Override
    @Nullable
    public ModelPart accessories$pollRoot() {
        return accessories$modelPartStorage.poll();
    }

    @Override
    public void accessories$clearQueue() {
        accessories$modelPartStorage.clear();
    }

    //--

    @WrapOperation(method = "bakeLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/builders/LayerDefinition;bakeRoot()Lnet/minecraft/client/model/geom/ModelPart;"))
    private ModelPart accessories$saveRootPart(LayerDefinition instance, Operation<ModelPart> original) {
        var part = original.call(instance);

        accessories$pushRoot(part);

        return part;
    }
}
