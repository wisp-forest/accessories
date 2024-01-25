package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface AccessoryRenderer {

    <T extends LivingEntity, M extends EntityModel<T>> void render(
            boolean isRendering,
            ItemStack stack,
            SlotReference reference,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float netHeadYaw,
            float headPitch
    );


    /**
     * Translates the rendering context to the center of the player's face
     */
    static void translateToFace(PoseStack matrices, HumanoidModel<LivingEntity> model, LivingEntity entity, float headYaw, float headPitch) {

        if (entity.isVisuallySwimming() || entity.isFallFlying()) {
            matrices.mulPose(Axis.ZP.rotationDegrees(model.head.zRot));
            matrices.mulPose(Axis.YP.rotationDegrees(headYaw));
            matrices.mulPose(Axis.XP.rotationDegrees(-45.0F));
        } else {

            if (entity.isCrouching() && !model.riding) {
                matrices.translate(0.0F, 0.25F, 0.0F);
            }
            matrices.mulPose(Axis.YP.rotationDegrees(headYaw));
            matrices.mulPose(Axis.XP.rotationDegrees(headPitch));
        }
        matrices.translate(0.0F, -0.25F, -0.3F);
    }
}
