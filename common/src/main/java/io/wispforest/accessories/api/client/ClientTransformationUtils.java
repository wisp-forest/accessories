package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

public class ClientTransformationUtils {

    @Environment(EnvType.CLIENT)
    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, HumanoidModel<? extends LivingEntity> model, Runnable renderCall) {
        poseStack.pushPose();

        transformStack(transformations, poseStack, model);

        renderCall.run();

        poseStack.popPose();
    }

    @Environment(EnvType.CLIENT)
    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, HumanoidModel<? extends LivingEntity> model) {
        for (var transformation : transformations) {
            transform(transformation, poseStack, model);
        }
    }

    @Environment(EnvType.CLIENT)
    private static void transform(Transformation value, PoseStack poseStack, HumanoidModel<? extends LivingEntity> model) {
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
    private static ModelPart getPart(String partKey, HumanoidModel<? extends LivingEntity> model) {
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
