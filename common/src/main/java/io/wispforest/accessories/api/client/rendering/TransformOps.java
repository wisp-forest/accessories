package io.wispforest.accessories.api.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;

import java.util.List;

//@Environment(EnvType.CLIENT)
@ApiStatus.Experimental
public class TransformOps {

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, EntityModel<? extends LivingEntityRenderState> model, Runnable renderCall) {
        poseStack.pushPose();

        transformStack(transformations, poseStack, model);

        try {
            renderCall.run();
        } finally {
            poseStack.popPose();
        }
    }

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, EntityModel<? extends LivingEntityRenderState> model) {
        for (var transformation : transformations) {
            transform(transformation, poseStack, model);
        }
    }

    private static void transform(Transformation value, PoseStack poseStack, EntityModel<? extends LivingEntityRenderState> model) {
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

                var part = ModelTransformOps.getPart(model, modelTarget.modelPart());

                if (part != null) {
                    if (modelTarget.rawNormal() != null) {
                        var axisTranslations = modelTarget.rawNormal();

                        ModelTransformOps.transformToModelPart(poseStack, part, axisTranslations.x, axisTranslations.y, axisTranslations.z);
                    } else if (modelTarget.side() != null) {
                        ModelTransformOps.transformToFace(poseStack, part, modelTarget.side());
                    } else {
                        ModelTransformOps.transformToModelPart(poseStack, part);
                    }
                }
            }
            case Transformation.Matrix matrix -> poseStack.mulPose(matrix.matrix4f());
            case null, default -> throw new IllegalStateException("A invalid transform was created meaning such is unable to be encoded!");
        }
    }
}
