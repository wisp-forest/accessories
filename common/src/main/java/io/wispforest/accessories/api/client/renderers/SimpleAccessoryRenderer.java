package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Simple version of the {@link AccessoryRenderer} with a transformation method to adjust
 * a given item to certain part of the given {@link LivingEntity} then rendering the accessory
 * as an Item at the specified location and scale
 */
public interface SimpleAccessoryRenderer extends AccessoryRenderer {

    @Override
    default <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks){
        align(stack, path, model, renderState, matrices);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, null, 0);
    }

    /**
     * Method used to align the given matrices to the desired position and scale on the current {@link LivingEntity}
     * passed within the {@link #render} method.
     *
     * @param stack
     * @param path
     * @param model
     * @param matrices
     * @param <S>
     */
    <S extends LivingEntityRenderState> void align(ItemStack stack, SlotPath path, EntityModel<S> model, S renderState, PoseStack matrices);

}