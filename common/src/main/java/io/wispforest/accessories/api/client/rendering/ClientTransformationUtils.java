package io.wispforest.accessories.api.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.client.Transformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;

import java.util.List;

@Environment(EnvType.CLIENT)
@ApiStatus.Experimental
public class ClientTransformationUtils {

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, LivingEntity entity, EntityModel<? extends LivingEntity> model, Runnable renderCall) {
        poseStack.pushPose();

        transformStack(transformations, poseStack, entity, model);

        try {
            renderCall.run();
        } finally {
            poseStack.popPose();
        }

    }

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, LivingEntity entity, EntityModel<? extends LivingEntity> model) {
        for (var transformation : transformations) {
            transform(transformation, poseStack, entity, model);
        }
    }

    private static void transform(Transformation value, PoseStack poseStack, LivingEntity entity, EntityModel<? extends LivingEntity> model) {
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

                if (modelTarget.rawNormal() != null) {
                    var axisTranslations = modelTarget.rawNormal();

                    ModelTransformUtils.transformToModelPart(poseStack, entity, model, modelTarget.modelPart(), axisTranslations.x, axisTranslations.y, axisTranslations.z);
                } else if (modelTarget.side() != null) {
                    ModelTransformUtils.transformToFace(poseStack, entity, model, modelTarget.modelPart(), modelTarget.side());
                } else {
                    ModelTransformUtils.transformToModelPart(poseStack, entity, model, modelTarget.modelPart());
                }
            }
            case Transformation.Matrix matrix -> poseStack.mulPose(matrix.matrix4f());
            case null, default -> throw new IllegalStateException("A invalid transform was created meaning such is unable to be encoded!");
        }
    }
}
