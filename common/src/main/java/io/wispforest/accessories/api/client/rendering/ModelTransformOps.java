package io.wispforest.accessories.api.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import io.wispforest.accessories.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//@Environment(EnvType.CLIENT)
@ApiStatus.Experimental
public class ModelTransformOps {

    private static final Map<ResourceLocation, ModelPartTransformer> ADDITIONAL_TRANSFORMERS = new LinkedHashMap<>();

    @ApiStatus.Experimental
    public static void registerTransformer(ResourceLocation location, ModelPartTransformer modelTransformers) {
        if (ADDITIONAL_TRANSFORMERS.containsKey(location)) {
            throw new IllegalStateException("Already existing ModelTransformer exists!");
        }

        ADDITIONAL_TRANSFORMERS.put(location, modelTransformers);
    }

    @ApiStatus.Experimental
    public static boolean transformToFace(PoseStack poseStack, LivingEntityRenderState renderState, Model model, String modelPartName, Side side) {
        var vec = side.direction.getUnitVec3i();

        return transformToModelPart(poseStack, renderState, model, modelPartName, vec.getX(), vec.getY(), vec.getZ());
    }

    @ApiStatus.Experimental
    public static boolean transformToModelPart(PoseStack poseStack, LivingEntityRenderState renderState, Model model, String modelPartName) {
        return transformToModelPart(poseStack, renderState, model, modelPartName, 0, 0, 0);
    }

    @ApiStatus.Experimental
    public static boolean transformToModelPart(PoseStack poseStack, LivingEntityRenderState renderState, Model model, String modelPartName, @Nullable Number xPercent, @Nullable Number yPercent, @Nullable Number zPercent) {
        // TODO: ADD ERRORING FOR IF IT OCCURED

        for (var entry : ADDITIONAL_TRANSFORMERS.entrySet()) {
            var result = entry.getValue().transformToPart(poseStack, renderState, model, modelPartName, xPercent, yPercent, zPercent);

            if (result) return true;
        }

        var modelPart = getPart(model, modelPartName);

        if (modelPart != null) {
            transformToModelPart(poseStack, modelPart, xPercent, yPercent, zPercent);

            return true;
        }

        return false;
    }

    @ApiStatus.Experimental
    @Nullable
    public static ModelPart getPart(Model model, String modelPartName) {
        var possiblePart = getAnyDescendantWithName(model, modelPartName);

        return possiblePart.orElse(null);
    }

    public static Optional<ModelPart> getAnyDescendantWithName(Model model, String name) {
        var root = model.root();

        if (name.equals("root")) return Optional.of(root);

        return getAnyDescendantWithName(root, name);
    }

    private static Optional<ModelPart> getAnyDescendantWithName(ModelPart part, String name) {
        for (var entry : ((ModelPartAccessor) (Object) part).accessories$getChildren().entrySet()) {
            var childName = entry.getKey();
            var childPart = entry.getValue();

            if (childName.equals(name)) return Optional.of(childPart);

            var result = getAnyDescendantWithName(childPart, name);

            if (result.isPresent()) return result;
        }

        return Optional.empty();
    }

    /**
     * Transforms the rendering context to a specific face on a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     * @param side      The side of the ModelPart to transform to
     */
    public static void transformToFace(PoseStack poseStack, ModelPart part, Side side) {
        var vec = side.direction.getUnitVec3i();

        transformToModelPart(poseStack, part, vec.getX(), vec.getY(), vec.getZ());
    }

    /**
     * Transforms the rendering context to the center of a ModelPart
     *
     * @param poseStack the pose stack to apply the transformation(s) to
     * @param part      The ModelPart to transform to
     */
    public static void transformToModelPart(PoseStack poseStack, ModelPart part) {
        transformToModelPart(poseStack, part, 0, 0, 0);
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
    public static void transformToModelPart(PoseStack poseStack, ModelPart part, @Nullable Number xPercent, @Nullable Number yPercent, @Nullable Number zPercent) {
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

    private static Pair<Vec3, Vec3> getAABB(ModelPart part) {
        Vec3 min = new Vec3(0, 0, 0);
        Vec3 max = new Vec3(0, 0, 0);

        if (part.getClass().getSimpleName().contains("EMFModelPart")) {
            var parts = new ArrayList<ModelPart>();

            parts.add(part);
            parts.addAll(((ModelPartAccessor) (Object) part).accessories$getChildren().values());

            for (var modelPart : parts) {
                for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) modelPart).accessories$getCubes()) {
                    min = new Vec3(
                            Math.min(min.x, Math.min(cube.minX + modelPart.x, cube.maxX + modelPart.x)),
                            Math.min(min.y, Math.min(cube.minY + modelPart.y, cube.maxY + modelPart.y)),
                            Math.min(min.z, Math.min(cube.minZ + modelPart.z, cube.maxZ + modelPart.z))
                    );
                    max = new Vec3(
                            Math.max(max.x, Math.max(cube.minX + modelPart.x, cube.maxX + modelPart.x)),
                            Math.max(max.y, Math.max(cube.minY + modelPart.y, cube.maxY + modelPart.y)),
                            Math.max(max.z, Math.max(cube.minZ + modelPart.z, cube.maxZ + modelPart.z))
                    );
                }
            }
        } else {
            for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) part).accessories$getCubes()) {
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
        }

        return Pair.of(min, max);
    }

    @ApiStatus.Experimental
    public interface ModelPartTransformer {
        boolean transformToPart(PoseStack poseStack, LivingEntityRenderState renderState, Model model, String modelPartName, @Nullable Number xPercent, @Nullable Number yPercent, @Nullable Number zPercent);
    }
}
