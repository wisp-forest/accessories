package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

@Environment(EnvType.CLIENT)
@ApiStatus.Experimental
public class ClientTransformationUtils {

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, EntityModel<? extends LivingEntity> model, Runnable renderCall) {
        poseStack.pushPose();

        transformStack(transformations, poseStack, model);

        renderCall.run();

        poseStack.popPose();
    }

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, EntityModel<? extends LivingEntity> model) {
        for (var transformation : transformations) {
            transform(transformation, poseStack, model);
        }
    }

    private static void transform(Transformation value, PoseStack poseStack, EntityModel<? extends LivingEntity> model) {
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
    private static ModelPart getPart(String partKey, EntityModel<? extends LivingEntity> model) {
        if (partKey.equals("head") && model instanceof HeadedModel headedModel) {
            return headedModel.getHead();
        }

        if (model instanceof HumanoidModel<? extends LivingEntity> humanoidModel) {
            return switch (partKey) {
                case "hat" -> humanoidModel.hat;
                case "body" -> humanoidModel.body;
                case "rightArm" -> humanoidModel.rightArm;
                case "leftArm" -> humanoidModel.leftArm;
                case "rightLeg" -> humanoidModel.rightLeg;
                case "leftLeg" -> humanoidModel.leftLeg;
                default -> {
                    // TOOD: Handle error by log or something?
                    //throw new IllegalStateException("Unable to locate the given model part for the given model!: " + partKey);

                    yield null;
                }
            };
        }

        return null;
    }
}
