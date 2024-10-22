package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import io.wispforest.accessories.mixin.client.ModelPartAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TransformOps {

    public static void transformToFace(PoseStack poseStack, ModelPart part, Side side) {
        transformToModelPart(poseStack, part, side.direction.getStepX(), side.direction.getStepY(), side.direction.getStepZ());
    }

    public static void transformToModelPart(PoseStack poseStack, ModelPart part) {
        transformToModelPart(poseStack, part, 0, 0, 0);
    }

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
            parts.addAll(((ModelPartAccessor) (Object) part).getChildren().values());

            for (var modelPart : parts) {
                for (ModelPart.Cube cube : ((ModelPartAccessor) (Object) modelPart).getCubes()) {
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
        }

        return Pair.of(min, max);
    }

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model, Runnable renderCall) {
        poseStack.pushPose();

        transformStack(transformations, poseStack, model);

        renderCall.run();

        poseStack.popPose();
    }

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model) {
        for (var transformation : transformations) {
            transform(transformation, poseStack, model);
        }
    }

    private static void transform(Transformation value, PoseStack poseStack, HumanoidModel<? extends HumanoidRenderState> model) {
        switch (value) {
            case Transformation.Translation translation -> {
                var translationVector = translation.translation();

                poseStack.translate(translationVector.x, translationVector.y, translationVector.z);
            }
            case Transformation.RawRotation rawRotation -> poseStack.mulPose(rawRotation.quarternionf());
            case Transformation.AxisRotation axisRotation -> poseStack.mulPose(axisRotation.axisAngle4f().get(new Quaternionf()));
            case Transformation.Scale scale -> {
                var scaleVector = scale.scale();

                poseStack.scale(scaleVector.x, scaleVector.y, scaleVector.z);
            }
            case Transformation.TransformTo transformTo -> {
                var modelTarget = transformTo.target();

                var part = getPart(modelTarget.modelPart(), model);

                if (part != null) {
                    if (modelTarget.rawNormal() != null) {
                        var axisTranslations = modelTarget.rawNormal();

                        AccessoryRenderer.transformToModelPart(poseStack, part, axisTranslations.x, axisTranslations.y, axisTranslations.z);
                    } else if (modelTarget.side() != null) {
                        AccessoryRenderer.transformToFace(poseStack, part, modelTarget.side());
                    } else {
                        AccessoryRenderer.transformToModelPart(poseStack, part);
                    }
                }
            }
            case Transformation.Matrix matrix -> poseStack.mulPose(matrix.matrix4f());
            case null, default -> throw new IllegalStateException("A invalid transform was created meaning such is unable to be encoded!");
        }
    }

    @Nullable
    @Environment(EnvType.CLIENT)
    private static ModelPart getPart(String partKey, HumanoidModel<? extends HumanoidRenderState> model) {
        return switch (partKey) {
            case "head" -> model.head;
            case "hat" -> model.hat;
            case "body" -> model.body;
            case "rightArm" -> model.rightArm;
            case "leftArm" -> model.leftArm;
            case "rightLeg" -> model.rightLeg;
            case "leftLeg" -> model.leftLeg;
            default -> {
                // TOOD: Handle error by log or something?
                //throw new IllegalStateException("Unable to locate the given model part for the given model!: " + partKey);

                yield null;
            }
        };
    }
}
