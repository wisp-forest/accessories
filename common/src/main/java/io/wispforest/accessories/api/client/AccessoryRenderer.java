package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Main Render Interface used to render Accessories
 *
 * <p/>
 * All Translation code is based on <a href="https://github.com/emilyploszaj/trinkets/blob/main/src/main/java/dev/emi/trinkets/api/client/TrinketRenderer.java">TrinketRenderer</a>
 * with adjustments to allow for any {@link LivingEntity} extending {@link HumanoidModel} in which
 * credit goes to <a href="https://github.com/TheIllusiveC4">TheIllusiveC4</a>, <a href="https://github.com/florensie">florensie</a>, and <a href="https://github.com/emilyploszaj">Emi</a>
 */
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
     * Rotates the rendering for the models based on the entity's poses and movements. This will do
     * nothing if the entity render object does not implement {@link LivingEntityRenderer} or if the
     * model does not implement {@link HumanoidModel}).
     *
     * @param entity The wearer of the trinket
     * @param model The model to align to the body movement
     */
    @SuppressWarnings("unchecked")
    static void followBodyRotations(final LivingEntity entity, final HumanoidModel<LivingEntity> model) {
        EntityRenderer<? super LivingEntity> render = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        if (render instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof HumanoidModel entityModel) {
            entityModel.copyPropertiesTo(model);
        }
    }

    /**
     * Translates the rendering context to the center of the player's face
     */
    static void translateToFace(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity entity, float headYaw, float headPitch) {
        if (entity.isVisuallySwimming() || entity.isFallFlying()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(model.head.zRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(headYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
        } else {
            if (entity.isCrouching() && !model.riding) poseStack.translate(0.0F, 0.25F, 0.0F);

            poseStack.mulPose(Axis.YP.rotationDegrees(headYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
        }

        poseStack.translate(0.0F, -0.25F, -0.3F);
    }

    /**
     * Translates the rendering context to the center of the player's chest/torso segment
     */
    static void translateToChest(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity livingEntity) {
        if (livingEntity.isCrouching() && !model.riding && !livingEntity.isSwimming()) {
            poseStack.translate(0.0F, 0.2F, 0.0F);
            poseStack.mulPose(Axis.XP.rotation(model.body.xRot));
        }

        poseStack.mulPose(Axis.ZP.rotation(model.body.yRot));
        poseStack.translate(0.0F, 0.4F, -0.16F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right arm
     */
    static void translateToRightArm(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.2F, 0.0F);

        poseStack.mulPose(Axis.ZP.rotation(model.body.yRot));
        poseStack.translate(-0.3125F, 0.15625F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(model.rightArm.zRot));
        poseStack.mulPose(Axis.ZP.rotation(model.rightArm.yRot));
        poseStack.mulPose(Axis.XP.rotation(model.rightArm.xRot));
        poseStack.translate(-0.0625F, 0.625F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left arm
     */
    static void translateToLeftArm(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.2F, 0.0F);

        poseStack.mulPose(Axis.ZP.rotation(model.body.yRot));
        poseStack.translate(0.3125F, 0.15625F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(model.leftArm.zRot));
        poseStack.mulPose(Axis.ZP.rotation(model.leftArm.yRot));
        poseStack.mulPose(Axis.XP.rotation(model.leftArm.xRot));
        poseStack.translate(0.0625F, 0.625F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right leg
     */
    static void translateToRightLeg(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.0F, 0.25F);

        poseStack.translate(-0.125F, 0.75F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(model.rightLeg.zRot));
        poseStack.mulPose(Axis.ZP.rotation(model.rightLeg.yRot));
        poseStack.mulPose(Axis.XP.rotation(model.rightLeg.xRot));
        poseStack.translate(0.0F, 0.75F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left leg
     */
    static void translateToLeftLeg(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.0F, 0.25F);

        poseStack.translate(0.125F, 0.75F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(model.leftLeg.zRot));
        poseStack.mulPose(Axis.ZP.rotation(model.leftLeg.yRot));
        poseStack.mulPose(Axis.XP.rotation(model.leftLeg.xRot));
        poseStack.translate(0.0F, 0.75F, 0.0F);
    }
}
