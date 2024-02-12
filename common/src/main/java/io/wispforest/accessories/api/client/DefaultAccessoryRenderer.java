package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class DefaultAccessoryRenderer implements AccessoryRenderer {

    @Override
    public <M extends LivingEntity> void render(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(!isRendering) return;
        if (!(model instanceof HumanoidModel<? extends LivingEntity> humanoidModel)) return;

        Consumer<PoseStack> render = (poseStack) -> Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, reference.entity().level(), 0);

        var slotName = reference.slotName();
        switch (slotName) {
            case "face" -> {
                AccessoryRenderer.transformToFace(matrices, humanoidModel.head, Side.FRONT);
                render.accept(matrices);
            }
            case "hat" -> {
                AccessoryRenderer.transformToFace(matrices, humanoidModel.head, Side.TOP);
                matrices.translate(0, 0.25, 0);
                render.accept(matrices);
            }
            case "back" -> {
                AccessoryRenderer.transformToFace(matrices, humanoidModel.body, Side.BACK);
                matrices.scale(1.5f, 1.5f, 1.5f);
                render.accept(matrices);
            }
            case "necklace" -> {
                AccessoryRenderer.transformToModelPart(matrices, humanoidModel.body, 0, 1, 1);
                matrices.translate(0, -0.25, 0);
                render.accept(matrices);
            }
            case "cape" -> {
                AccessoryRenderer.transformToModelPart(matrices, humanoidModel.body, 0, 1, -1);
                matrices.translate(0, -0.25, 0);
                render.accept(matrices);
            }
            case "ring" -> {
                AccessoryRenderer.transformToModelPart(matrices, reference.slot() % 2 == 0 ? humanoidModel.rightArm : humanoidModel.leftArm, reference.slot() % 2 == 0 ? 1 : -1, -1, 0);
                matrices.translate(0, 0.25, 0);
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.mulPose(Axis.YP.rotationDegrees(90));
                render.accept(matrices);
            }
            case "wrist" -> {
                AccessoryRenderer.transformToModelPart(matrices, reference.slot() % 2 == 0 ? humanoidModel.rightArm : humanoidModel.leftArm, 0, -0.5, 0);
                matrices.scale(1.01f, 1.01f, 1.01f);
                matrices.mulPose(Axis.YP.rotationDegrees(90));
                render.accept(matrices);
            }
            case "hand" -> {
                AccessoryRenderer.transformToFace(matrices, reference.slot() % 2 == 0 ? humanoidModel.rightArm : humanoidModel.leftArm,Side.BOTTOM);
                matrices.translate(0, 0.25, 0);
                matrices.scale(1.02f, 1.02f, 1.02f);
                matrices.mulPose(Axis.YP.rotationDegrees(90));
                render.accept(matrices);
            }
            case "belt" -> {
                AccessoryRenderer.transformToFace(matrices, humanoidModel.body, Side.BOTTOM);
                matrices.scale(1.01f, 1.01f, 1.01f);
                render.accept(matrices);
            }
            case "anklet" -> {
                AccessoryRenderer.transformToModelPart(matrices, reference.slot() % 2 == 0 ? humanoidModel.rightLeg : humanoidModel.leftLeg, 0, -0.5, 0);
                matrices.scale(1.01f, 1.01f, 1.01f);
                render.accept(matrices);
            }
            case "shoes" -> {
                matrices.pushPose();
                AccessoryRenderer.transformToFace(matrices, humanoidModel.rightLeg, Side.BOTTOM);
                matrices.translate(0, 0.25, 0);
                matrices.scale(1.02f, 1.02f, 1.02f);
                render.accept(matrices);
                matrices.popPose();
                matrices.pushPose();
                AccessoryRenderer.transformToFace(matrices, humanoidModel.leftLeg, Side.BOTTOM);
                matrices.translate(0, 0.25, 0);
                matrices.scale(1.02f, 1.02f, 1.02f);
                render.accept(matrices);
                matrices.popPose();
            }
        }
    }


    @Override
    public boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
        return (reference.slotName().equals("hand") || reference.slotName().equals("wrist") || reference.slotName().equals("ring")) && (reference.slot() % 2 == 0 ? arm == HumanoidArm.RIGHT : arm == HumanoidArm.LEFT);
    }
}