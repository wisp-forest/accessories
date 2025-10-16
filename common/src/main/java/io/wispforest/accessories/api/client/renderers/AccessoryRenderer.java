package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.client.AccessoryRenderState;
import io.wispforest.accessories.api.client.rendering.ModelTransformOps;
import io.wispforest.accessories.api.client.rendering.Side;
import io.wispforest.accessories.api.slot.SlotPath;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

///
/// A renderer implementation used to render [ItemStack]s equipped within a [LivingEntity]'s [AccessoriesStorage].
///
public interface AccessoryRenderer {

    ///
    /// Primary render function used to render an equipped [ItemStack] on a given [LivingEntity]. The [SlotPath] parameter referees
    /// to where the given passed [ItemStack] is currently equipped into.
    ///
    /// \[1.21 and below -> 1.21.2]
    /// - limbSwing       -> {@link LivingEntityRenderState#walkAnimationPos}
    /// - limbSwingAmount -> {@link LivingEntityRenderState#walkAnimationSpeed}
    /// - ageInTicks      -> {@link LivingEntityRenderState#ageInTicks}
    /// - netHeadYaw      -> {@link LivingEntityRenderState#yRot}
    /// - headPitch       -> {@link LivingEntityRenderState#xRot}
    ///
    /// \[1.21.8 and below -> 1.21.9]
    /// - stack           -> {@link AccessoryRenderState#getStateData} with {@link AccessoriesRenderStateKeys#ITEM_STACK}
    /// - path            -> {@link AccessoryRenderState#getStateData} with {@link AccessoriesRenderStateKeys#SLOT_PATH}
    /// - light           -> {@link LivingEntityRenderState#getStateData} with {@link AccessoriesRenderStateKeys#LIGHT}
    /// - partialTicks    -> {@link LivingEntityRenderState#getStateData} with {@link AccessoriesRenderStateKeys#PARTIAL_TICKS}
    /// - arm             -> {@link LivingEntityRenderState#getStateData} with {@link AccessoriesRenderStateKeys#ARM}
    ///
    public <S extends LivingEntityRenderState> void render(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices, SubmitNodeCollector collector);

    //--

    @Nullable
    default AccessoryRenderState createRenderState(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState) {
        var isRenderingEnabled = storageLookup.getFromContainer(path, AccessoriesStorage::shouldRender);

        if (!shouldRender(stack, path, storageLookup, entity, entityState, isRenderingEnabled != null ? isRenderingEnabled : true)) return null;

        var accessoryState = AccessoryRenderState.setupState(path, stack, entity, entityState, shouldCreateStackRenderState());

        extractRenderState(stack, path, storageLookup, entity, entityState, accessoryState);

        return accessoryState;
    }

    default void extractRenderState(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, AccessoryRenderState accessoryState) {}

    default boolean shouldCreateStackRenderState() {
        return false;
    }

    ///
    /// Method used to test if the given accessory should be rendered based on the given context which controls if extraction
    /// to a `AccessoryRenderState` will occur causing a render.
    ///
    /// This **replaces** the old `shouldRenderInFirstPerson` as you can get the [HumanoidArm] `arm` value using [LivingEntityRenderState#getStateData]
    /// with [AccessoriesRenderStateKeys#ARM]
    ///
    default boolean shouldRender(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, boolean isRenderingEnabled) {
        if (entityState.hasStateData(AccessoriesRenderStateKeys.ARM)) {
            return false;
        }

        return isRenderingEnabled;
    }

    //--

    /**
     * Transforms the rendering context to a specific face on a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     * @param side      The side of the ModelPart to transform to
     */
    static void transformToFace(PoseStack poseStack, ModelPart part, Side side) {
        ModelTransformOps.transformToFace(poseStack, part, side);
    }

    /**
     * Transforms the rendering context to the center of a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     */
    static void transformToModelPart(PoseStack poseStack, ModelPart part) {
        ModelTransformOps.transformToModelPart(poseStack, part, 0, 0, 0);
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
        ModelTransformOps.transformToModelPart(poseStack, part, xPercent, yPercent, zPercent);
    }

    //--

    @ApiStatus.NonExtendable
    default boolean isEmpty() {
        return this instanceof BuiltinAccessoryRenderers.EmptyRenderer;
    }
}
