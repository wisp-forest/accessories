package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Simple version of the {@link AccessoryRenderer} with a transformation method to adjust
 * a given item to certain part of the given {@link LivingEntity} then rendering such as
 * an Item at such location and scale
 */
public interface SimpleAccessoryRenderer extends AccessoryRenderer {

    @Override
    default <M extends LivingEntity> void render(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch){
        if(!isRendering) return;

        align(stack, reference, model, matrices);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, reference.entity().level(), 0);
    }

    /**
     * Method used to align the given matrices to the desired position and scale on the current {@link LivingEntity}
     * passed within the {@link #render} method.
     *
     * @param stack
     * @param reference
     * @param model
     * @param matrices
     * @param <M>
     */
    <M extends LivingEntity> void align(ItemStack stack, SlotReference reference, EntityModel<M> model, PoseStack matrices);

}