package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.client.rendering.ModelTransformUtils;
import io.wispforest.accessories.mixin.client.ModelPartAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TransformOps {

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, HumanoidModel<? extends LivingEntityRenderState> model, Runnable renderCall) {
        poseStack.pushPose();

        transformStack(transformations, poseStack, model);

        renderCall.run();

        poseStack.popPose();
    }

    public static void transformStack(List<Transformation> transformations, PoseStack poseStack, HumanoidModel<? extends LivingEntityRenderState> model) {
        for (var transformation : transformations) {
            transform(transformation, poseStack, model);
        }
    }

    private static void transform(Transformation value, PoseStack poseStack, HumanoidModel<? extends LivingEntityRenderState> model) {
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

                var part = ModelTransformUtils.getPart(model, modelTarget.modelPart());

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
}
