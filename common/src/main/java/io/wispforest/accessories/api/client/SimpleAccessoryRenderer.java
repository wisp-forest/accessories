package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface SimpleAccessoryRenderer extends AccessoryRenderer {

    @Override
    default <M extends LivingEntity> void render(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float netHeadYaw, float headPitch){
        if(!isRendering) return;

        align(stack, reference, model, matrices);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, reference.entity().level(), 0);
    }

    <M extends LivingEntity> void align(ItemStack stack, SlotReference reference, EntityModel<M> model, PoseStack matrices);

    /**
     * Determines if this accessory should render in first person
     * Override to return true for whichever arm this accessory renders on
     */
    default boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
        return false;
    }

    @Override
    default <M extends LivingEntity> void renderOnFirstPersonRightArm(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light) {
        if (!shouldRenderInFirstPerson(HumanoidArm.RIGHT, stack, reference)) return;
        AccessoryRenderer.super.renderOnFirstPersonRightArm(isRendering, stack, reference, matrices, model, multiBufferSource, light);
    }

    @Override
    default <M extends LivingEntity> void renderOnFirstPersonLeftArm(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light) {
        if (!shouldRenderInFirstPerson(HumanoidArm.LEFT, stack, reference)) return;
        AccessoryRenderer.super.renderOnFirstPersonLeftArm(isRendering, stack, reference, matrices, model, multiBufferSource, light);
    }
}