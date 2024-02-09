package io.wispforest.testccessories.fabric.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistery;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TntAccessory implements Accessory {

    @Environment(EnvType.CLIENT)
    public static void clientInit(){
        AccessoriesRendererRegistery.registerRenderer(Items.TNT, new TntAccessory.Renderer());
    }

    public static void init(){
        AccessoriesAPI.registerAccessory(Items.TNT, new TntAccessory());
    }

    @Environment(EnvType.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void align(LivingEntity entity, M model, PoseStack matrices, float netHeadYaw, float headPitch) {
            if(!(model instanceof HeadedModel headedModel)) return;

            AccessoryRenderer.transformToModelPart(matrices, headedModel.getHead(), null, 1, null);
        }

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float netHeadYaw, float headPitch) {
            if (!isRendering) return;

            align(reference.entity(), renderLayerParent.getModel(), poseStack, netHeadYaw, headPitch);
            poseStack.scale(2, 2, 2);
            poseStack.translate(0, 1/4f, 0);
            for (int i = 0; i < stack.getCount(); i++) {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, multiBufferSource, reference.entity().level(), 0);
                poseStack.translate(0, 1/2f, 0);
            }
        }
    }
}