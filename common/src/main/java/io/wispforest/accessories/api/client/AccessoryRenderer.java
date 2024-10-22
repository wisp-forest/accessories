package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.mixin.client.LivingEntityRendererAccessor;
import io.wispforest.accessories.mixin.client.ModelPartAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.stream.Stream;

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
     * </br></br>
     * <pre>  [1.21 and below -> 1.21.2]
     * limbSwing       -> {@link LivingEntityRenderState#walkAnimationPos}
     * limbSwingAmount -> {@link LivingEntityRenderState#walkAnimationSpeed}
     * ageInTicks      -> {@link LivingEntityRenderState#ageInTicks}
     * netHeadYaw      -> {@link LivingEntityRenderState#yRot}
     * headPitch       -> {@link LivingEntityRenderState#xRot}</pre>
     */
    <S extends LivingEntityRenderState> void render(
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<S> model,
            S renderState,
            MultiBufferSource multiBufferSource,
            int light,
            float partialTicks
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
    default <S extends LivingEntityRenderState> void renderOnFirstPerson(
            HumanoidArm arm,
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<S> model,
            S renderState,
            MultiBufferSource multiBufferSource,
            int light,
            float partialTicks
    ) {
        if (!shouldRenderInFirstPerson(arm, stack, reference)) return;

        this.render(stack, reference, matrices, model, renderState, multiBufferSource, light, partialTicks);
    }

    /**
     * Rotates the rendering for the models based on the entity's poses and movements. This will do
     * nothing if the entity render object does not implement {@link LivingEntityRenderer} or if the
     * model does not implement {@link HumanoidModel}).
     *
     * @param entity The wearer of the trinket
     * @param model  The model to align to the body movement
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true)
    static void followBodyRotations(final LivingEntity entity, final HumanoidModel<HumanoidRenderState> model) {
        EntityRenderer<? super LivingEntity, ?> render = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);

        if (render instanceof LivingEntityRenderer renderer && renderer.getModel() instanceof HumanoidModel entityModel) {
            entityModel.copyPropertiesTo(model);
        }
    }

    /**
     * Translates the rendering context to the center of the player's face
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @Deprecated
    static void translateToFace(PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model, LivingEntity entity) {
        transformToFace(poseStack, model.head, Side.FRONT);
    }

    /**
     * Translates the rendering context to the center of the player's chest/torso segment
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @Deprecated(forRemoval = true)
    static void translateToChest(PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model, LivingEntity livingEntity) {
        transformToModelPart(poseStack, model.body);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right arm
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @Deprecated(forRemoval = true)
    static void translateToRightArm(PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model, LivingEntity player) {
        transformToFace(poseStack, model.rightArm, Side.BOTTOM);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left arm
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @Deprecated(forRemoval = true)
    static void translateToLeftArm(PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model, LivingEntity player) {
        transformToFace(poseStack, model.leftArm, Side.BOTTOM);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's right leg
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @Deprecated(forRemoval = true)
    static void translateToRightLeg(PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model, LivingEntity player) {
        transformToFace(poseStack, model.rightLeg, Side.BOTTOM);
    }

    /**
     * Translates the rendering context to the center of the bottom of the player's left leg
     *
     * @deprecated Use {@link #transformToFace(PoseStack, ModelPart, Side)} or {@link #transformToModelPart(PoseStack, ModelPart)} instead
     */
    @Deprecated(forRemoval = true)
    static void translateToLeftLeg(PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model, LivingEntity player) {
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
        transformToModelPart(poseStack, part, side.direction.getStepX(), side.direction.getStepY(), side.direction.getStepZ());
    }

    /**
     * Transforms the rendering context to the center of a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     */
    static void transformToModelPart(PoseStack poseStack, ModelPart part) {
        TransformOps.transformToModelPart(poseStack, part, 0, 0, 0);
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
     */
    static void transformToModelPart(PoseStack poseStack, ModelPart part, @Nullable Number xPercent, @Nullable Number yPercent, @Nullable Number zPercent) {
        TransformOps.transformToModelPart(poseStack, part, xPercent, yPercent, zPercent);
    }
}
