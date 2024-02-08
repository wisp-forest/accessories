package io.wispforest.accessories.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public interface SimpleAccessoryRenderer extends AccessoryRenderer {

    @Override
    default <T extends LivingEntity, M extends EntityModel<T>> void render(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float netHeadYaw, float headPitch){
        if(!isRendering) return;

        align(reference.entity(), renderLayerParent.getModel(), poseStack, netHeadYaw, headPitch);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, reference.entity().level(), 0);
    }

    @Environment(EnvType.CLIENT)
    <T extends LivingEntity, M extends EntityModel<T>> void align(LivingEntity entity, M model, PoseStack matrices, float netHeadYaw, float headPitch);
}