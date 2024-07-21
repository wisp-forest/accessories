package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.mixin.client.ModelPartAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Main Render Interface used to render Accessories
 * <p>
 * All Translation code is based on <a href="https://github.com/emilyploszaj/trinkets/blob/main/src/main/java/dev/emi/trinkets/api/client/TrinketRenderer.java">TrinketRenderer</a>
 * with adjustments to allow for any {@link LivingEntity} extending {@link HumanoidModel} in which
 * credit goes to <a href="https://github.com/TheIllusiveC4">TheIllusiveC4</a>, <a href="https://github.com/florensie">florensie</a>, and <a href="https://github.com/emilyploszaj">Emi</a>
 */
public interface AccessoryRenderer {

    /**
     * Render method called within the {@link AccessoriesRenderLayer#render} when rendering a given Accessory on a given {@link LivingEntity}.
     * The given {@link SlotReference} refers to the slot based on its type, entity and index within the {@link AccessoriesContainer}.
     */
    <M extends LivingEntity> void render(
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    );

    /**
     * @return if the given Accessory should render or not based on the boolean provided
     */
    default boolean shouldRender(boolean isRendering) {
        return isRendering;
    }

    /**
     * Determines if this accessory should render in first person
     * Override to return true for whichever arm this accessory renders on
     */
    default boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
        return false;
    }

    /**
     * Attempt to render the given Accessory on the first person player model if found to be able to from the {@link #shouldRenderInFirstPerson}
     * invocation.
     */
    default <M extends LivingEntity> void renderOnFirstPerson(
            HumanoidArm arm,
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light
    ) {
        if (!shouldRenderInFirstPerson(arm, stack, reference)) return;

        this.render(stack, reference, matrices, model, multiBufferSource, light, 0, 0, 0, 0, 0, 0);
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
    @Deprecated
    static void followBodyRotations(final LivingEntity entity, final HumanoidModel<LivingEntity> model) {
        EntityRenderer<? super LivingEntity> render = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        if (render instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof HumanoidModel entityModel) {
            entityModel.copyPropertiesTo(model);
        }
    }

    /**
     * Translates the rendering context to the center of the player's face
     */
    @Deprecated
    static void translateToFace(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity entity) {
        transformToFace(poseStack, model.head, Side.FRONT);
    }

    /**
     * Translates the rendering context to the center of the player's chest/torso segment
     */
    @Deprecated
    static void translateToChest(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity livingEntity) {
        transformToModelPart(poseStack, model.body);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right arm
     */
    @Deprecated
    static void translateToRightArm(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.rightArm, Side.BOTTOM);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left arm
     */
    @Deprecated
    static void translateToLeftArm(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.leftArm, Side.BOTTOM);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right leg
     */
    @Deprecated
    static void translateToRightLeg(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.rightLeg, Side.BOTTOM);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left leg
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@Link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @Deprecated
    static void translateToLeftLeg(PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, LivingEntity player) {
        transformToFace(poseStack, model.leftLeg, Side.BOTTOM);
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

    enum transformType {
        POSITION,
        ROTATION,
        SCALE
    }

    /**
     * Transforms the rendering context to a specific place relative to a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     * @param xPercent  The percentage of the x-axis to translate to
     *                  <p>
     *                  (-1 being the left side and 1 being the right side)
     *                  <p>
     *                  If null, will be ignored
     * @param yPercent  The percentage of the y-axis to translate to
     *                  <p>
     *                  (-1 being the bottom and 1 being the top)
     *                  <p>
     *                  If null, will be ignored
     * @param zPercent  The percentage of the z-axis to translate to
     *                  <p>
     *                  (-1 being the back and 1 being the front)
     *                  <p>
     *                  If null, will be ignored
     * @param types     The types of transformations to apply
     */
    static void transformToModelPart(PoseStack poseStack, ModelPart part, @Nullable Number xPercent, @Nullable Number yPercent, @Nullable Number zPercent, transformType... types) {
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
        for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) part).getCubes()) {
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
