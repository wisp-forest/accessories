package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRenderMixin {
    @WrapOperation(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V")
    )
    private <S extends EntityRenderState> void accessories$passCameraRenderState(RenderLayer instance, PoseStack poseStack, SubmitNodeCollector collector, int i, S s, float f, float g, Operation<Void> original, @Local(argsOnly = true) CameraRenderState cameraState) {
        var bl = instance instanceof AccessoriesRenderLayer<?,?>;

        if (bl) ((LivingEntityRenderState) s).setStateData(AccessoriesRenderStateKeys.CAMERA_STATE, cameraState);

        original.call(instance, poseStack, collector, i, s, f, g);

        if (bl) ((LivingEntityRenderState) s).setStateData(AccessoriesRenderStateKeys.CAMERA_STATE, null);
    }
}
