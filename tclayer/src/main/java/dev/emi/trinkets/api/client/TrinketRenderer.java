package dev.emi.trinkets.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.emi.trinkets.api.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketRenderer {

    /**
     * Renders the Trinket
     *
     * @param stack The {@link ItemStack} for the Trinket being rendered
     * @param slotReference The exact slot for the item being rendered
     * @param contextModel The model this Trinket is being rendered on
     */
    void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
                PoseStack matrices, MultiBufferSource vertexConsumers, int light, LivingEntity entity,
                float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw,
                float headPitch);

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

        EntityRenderer<? super LivingEntity> render = Minecraft.getInstance()
                .getEntityRenderDispatcher().getRenderer(entity);

        if (render instanceof LivingEntityRenderer) {
            //noinspection unchecked
            LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> livingRenderer =
                    (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) render;
            EntityModel<LivingEntity> entityModel = livingRenderer.getModel();

            if (entityModel instanceof HumanoidModel) {
                HumanoidModel<LivingEntity> bipedModel = (HumanoidModel<LivingEntity>) entityModel;
                bipedModel.copyPropertiesTo(model);
            }
        }
    }

    /**
     * Translates the rendering context to the center of the player's face
     */
    static void translateToFace(PoseStack matrices, PlayerModel<AbstractClientPlayer> model,
                                AbstractClientPlayer player, float headYaw, float headPitch) {

        if (player.isSwimming() || player.isFallFlying()) {
            matrices.mulPose(Axis.ZP.rotationDegrees(model.head.zRot));
            matrices.mulPose(Axis.YP.rotationDegrees(headYaw));
            matrices.mulPose(Axis.XP.rotationDegrees(-45.0F));
        } else {

            if (player.isCrouching() && !model.riding) {
                matrices.translate(0.0F, 0.25F, 0.0F);
            }
            matrices.mulPose(Axis.YP.rotationDegrees(headYaw));
            matrices.mulPose(Axis.XP.rotationDegrees(headPitch));
        }
        matrices.translate(0.0F, -0.25F, -0.3F);
    }

    /**
     * Translates the rendering context to the center of the player's chest/torso segment
     */
    static void translateToChest(PoseStack matrices, PlayerModel<AbstractClientPlayer> model,
                                 AbstractClientPlayer player) {

        if (player.isCrouching() && !model.riding && !player.isSwimming()) {
            matrices.translate(0.0F, 0.2F, 0.0F);
            matrices.mulPose(Axis.XP.rotation(model.body.xRot));
        }
        matrices.mulPose(Axis.YP.rotation(model.body.yRot));
        matrices.translate(0.0F, 0.4F, -0.16F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right arm
     */
    static void translateToRightArm(PoseStack matrices, PlayerModel<AbstractClientPlayer> model,
                                    AbstractClientPlayer player) {

        if (player.isCrouching() && !model.riding && !player.isSwimming()) {
            matrices.translate(0.0F, 0.2F, 0.0F);
        }
        matrices.mulPose(Axis.YP.rotation(model.body.yRot));
        matrices.translate(-0.3125F, 0.15625F, 0.0F);
        matrices.mulPose(Axis.ZP.rotation(model.rightArm.zRot));
        matrices.mulPose(Axis.YP.rotation(model.rightArm.yRot));
        matrices.mulPose(Axis.XP.rotation(model.rightArm.xRot));
        matrices.translate(-0.0625F, 0.625F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left arm
     */
    static void translateToLeftArm(PoseStack matrices, PlayerModel<AbstractClientPlayer> model,
                                   AbstractClientPlayer player) {

        if (player.isCrouching() && !model.riding && !player.isSwimming()) {
            matrices.translate(0.0F, 0.2F, 0.0F);
        }
        matrices.mulPose(Axis.YP.rotation(model.body.yRot));
        matrices.translate(0.3125F, 0.15625F, 0.0F);
        matrices.mulPose(Axis.ZP.rotation(model.leftArm.zRot));
        matrices.mulPose(Axis.YP.rotation(model.leftArm.yRot));
        matrices.mulPose(Axis.XP.rotation(model.leftArm.xRot));
        matrices.translate(0.0625F, 0.625F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right leg
     */
    static void translateToRightLeg(PoseStack matrices, PlayerModel<AbstractClientPlayer> model,
                                    AbstractClientPlayer player) {

        if (player.isCrouching() && !model.riding && !player.isSwimming()) {
            matrices.translate(0.0F, 0.0F, 0.25F);
        }
        matrices.translate(-0.125F, 0.75F, 0.0F);
        matrices.mulPose(Axis.ZP.rotation(model.rightLeg.zRot));
        matrices.mulPose(Axis.YP.rotation(model.rightLeg.yRot));
        matrices.mulPose(Axis.XP.rotation(model.rightLeg.xRot));
        matrices.translate(0.0F, 0.75F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left leg
     */
    static void translateToLeftLeg(PoseStack matrices, PlayerModel<AbstractClientPlayer> model,
                                   AbstractClientPlayer player) {

        if (player.isCrouching() && !model.riding && !player.isSwimming()) {
            matrices.translate(0.0F, 0.0F, 0.25F);
        }
        matrices.translate(0.125F, 0.75F, 0.0F);
        matrices.mulPose(Axis.ZP.rotation(model.leftLeg.zRot));
        matrices.mulPose(Axis.YP.rotation(model.leftLeg.yRot));
        matrices.mulPose(Axis.XP.rotation(model.leftLeg.xRot));
        matrices.translate(0.0F, 0.75F, 0.0F);
    }
}