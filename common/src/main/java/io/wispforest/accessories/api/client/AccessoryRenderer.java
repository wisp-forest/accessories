package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Main Render Interface used to render Accessories
 * <p>
 * <p/>
 * All Translation code is based on <a href="https://github.com/emilyploszaj/trinkets/blob/main/src/main/java/dev/emi/trinkets/api/client/TrinketRenderer.java">TrinketRenderer</a>
 * with adjustments to allow for any {@link LivingEntity} extending {@link HumanoidModel} in which
 * credit goes to <a href="https://github.com/TheIllusiveC4">TheIllusiveC4</a>, <a href="https://github.com/florensie">florensie</a>, and <a href="https://github.com/emilyploszaj">Emi</a>
 */
public interface AccessoryRenderer {

    <M extends LivingEntity> void render(
            boolean isRendering,
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float netHeadYaw,
            float headPitch
    );

    default <M extends LivingEntity> void renderOnFirstPersonRightArm(
            boolean isRendering,
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light
    ) {
        this.render(
                isRendering,
                stack,
                reference,
                matrices,
                model,
                multiBufferSource,
                light,
                0,
                0,
                0,
                0,
                0
        );
    }

    default <M extends LivingEntity> void renderOnFirstPersonLeftArm(
            boolean isRendering,
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light
    ) {
        this.render(
                isRendering,
                stack,
                reference,
                matrices,
                model,
                multiBufferSource,
                light,
                0,
                0,
                0,
                0,
                0
        );
    }

    /**
     * Rotates the rendering for the models based on the entity's poses and movements. This will do
     * nothing if the entity render object does not implement {@link LivingEntityRenderer} or if the
     * model does not implement {@link HumanoidModel}).
     *
     * @param entity The wearer of the trinket
     * @param model  The model to align to the body movement
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
    static void translateToFace(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity entity) {
        transformToFace(poseStack, model.head, Side.FRONT);
//        if (entity.isVisuallySwimming() || entity.isFallFlying()) {
//            poseStack.mulPose(Axis.ZP.rotationDegrees(model.head.zRot));
//            poseStack.mulPose(Axis.YP.rotationDegrees(headYaw));
//            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F));
//        } else {
//            if (entity.isCrouching() && !model.riding) poseStack.translate(0.0F, 0.25F, 0.0F);
//
//            poseStack.mulPose(Axis.YP.rotationDegrees(headYaw));
//            poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
//        }
//
//        poseStack.translate(0.0F, -0.25F, -0.3F);
    }

    /**
     * Translates the rendering context to the center of the player's chest/torso segment
     */
    static void translateToChest(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity livingEntity) {
        transformToModelPart(poseStack, model.body);
//        if (livingEntity.isCrouching() && !model.riding && !livingEntity.isSwimming()) {
//            poseStack.translate(0.0F, 0.2F, 0.0F);
//            poseStack.mulPose(Axis.XP.rotation(model.body.xRot));
//        }
//
//        poseStack.mulPose(Axis.ZP.rotation(model.body.yRot));
//        poseStack.translate(0.0F, 0.4F, -0.16F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right arm
     */
    static void translateToRightArm(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.rightArm, Side.BOTTOM);
//        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.2F, 0.0F);
//
//        poseStack.mulPose(Axis.ZP.rotation(model.body.yRot));
//        poseStack.translate(-0.3125F, 0.15625F, 0.0F);
//        poseStack.mulPose(Axis.ZP.rotation(model.rightArm.zRot));
//        poseStack.mulPose(Axis.ZP.rotation(model.rightArm.yRot));
//        poseStack.mulPose(Axis.XP.rotation(model.rightArm.xRot));
//        poseStack.translate(-0.0625F, 0.625F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left arm
     */
    static void translateToLeftArm(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.leftArm, Side.BOTTOM);

//        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.2F, 0.0F);
//
//        poseStack.mulPose(Axis.ZP.rotation(model.body.yRot));
//        poseStack.translate(0.3125F, 0.15625F, 0.0F);
//        poseStack.mulPose(Axis.ZP.rotation(model.leftArm.zRot));
//        poseStack.mulPose(Axis.ZP.rotation(model.leftArm.yRot));
//        poseStack.mulPose(Axis.XP.rotation(model.leftArm.xRot));
//        poseStack.translate(0.0625F, 0.625F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right leg
     */
    static void translateToRightLeg(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.rightLeg, Side.BOTTOM);
//        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.0F, 0.25F);
//
//        poseStack.translate(-0.125F, 0.75F, 0.0F);
//        poseStack.mulPose(Axis.ZP.rotation(model.rightLeg.zRot));
//        poseStack.mulPose(Axis.ZP.rotation(model.rightLeg.yRot));
//        poseStack.mulPose(Axis.XP.rotation(model.rightLeg.xRot));
//        poseStack.translate(0.0F, 0.75F, 0.0F);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left leg
     */
    static void translateToLeftLeg(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.leftLeg, Side.BOTTOM);
//        if (player.isCrouching() && !model.riding && !player.isSwimming()) poseStack.translate(0.0F, 0.0F, 0.25F);
//
//        poseStack.translate(0.125F, 0.75F, 0.0F);
//        poseStack.mulPose(Axis.ZP.rotation(model.leftLeg.zRot));
//        poseStack.mulPose(Axis.ZP.rotation(model.leftLeg.yRot));
//        poseStack.mulPose(Axis.XP.rotation(model.leftLeg.xRot));
//        poseStack.translate(0.0F, 0.75F, 0.0F);
    }

    /**
     * Transforms the rendering context to a specific face on a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     * @param side      The side of the ModelPart to transform to
     */
    static void transformToFace(PoseStack poseStack, ModelPart part, Side side) {
        transformToModelPart(poseStack, part, side.direction.getNormal().getX(), side.direction.getNormal().getY(), side.direction.getNormal().getZ());
    }

    /**
     * Transforms the rendering context to a specific place relative to a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     * @param xPercent  The percentage of the x-axis to translate to
     *                  (-1 being the left side and 1 being the right side)
     *                  If null, will be ignored
     * @param yPercent  The percentage of the y-axis to translate to
     *                  (-1 being the bottom and 1 being the top)
     *                  If null, will be ignored
     * @param zPercent  The percentage of the z-axis to translate to
     *                  (-1 being the back and 1 being the front)
     *                  If null, will be ignored
     */
    static void transformToModelPart(PoseStack poseStack, ModelPart part, @Nullable Number xPercent, @Nullable Number yPercent, @Nullable Number zPercent) {
        part.translateAndRotate(poseStack);
        var aabb = getAABB(part);
        poseStack.scale(1 / 16f, 1 / 16f, 1 / 16f);
        poseStack.translate(
                xPercent != null ? Mth.lerp((-xPercent.doubleValue() + 1) / 2, aabb.getFirst().x, aabb.getSecond().x) : 0,
                yPercent != null ? Mth.lerp((-yPercent.doubleValue() + 1) / 2, aabb.getFirst().y, aabb.getSecond().y) : 0,
                zPercent != null ? Mth.lerp((-zPercent.doubleValue() + 1) / 2, aabb.getFirst().z, aabb.getSecond().z) : 0
        );
        poseStack.scale(8, 8, 8);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
    }

    /**
     * Transforms the rendering context to the center of a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     */
    static void transformToModelPart(PoseStack poseStack, ModelPart part) {
        transformToModelPart(poseStack, part, 0, 0, 0);
    }

    private static Pair<Vec3, Vec3> getAABB(ModelPart part) {
        Vec3 min = new Vec3(0, 0, 0);
        Vec3 max = new Vec3(0, 0, 0);
        for (ModelPart.Cube cube : part.cubes) {
            min = new Vec3(
                    Math.min(min.x, Math.min(cube.minX, cube.maxX)),
                    Math.min(min.y, Math.min(cube.minY, cube.maxY)),
                    Math.min(min.z, Math.min(cube.minZ, cube.maxZ))
            );
            max = new Vec3(
                    Math.max(max.x, Math.max(cube.minX, cube.maxX)),
                    Math.max(max.y, Math.max(cube.minY, cube.maxY)),
                    Math.max(max.z, Math.max(cube.minZ, cube.maxZ))
            );
        }
        return Pair.of(min, max);
    }
}