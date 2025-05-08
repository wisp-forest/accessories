package io.wispforest.testccessories.neoforge.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoryRegistry;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.client.renderers.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.neoforge.Testccessories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TntAccessory implements Accessory {

    @OnlyIn(Dist.CLIENT)
    public static void clientInit(){
        AccessoriesRendererRegistry.bindItemToRenderer(Items.TNT, Testccessories.of("tnt_head"), Renderer::new);
    }

    public static void init(){
        AccessoryRegistry.register(Items.TNT, new TntAccessory());
    }

    @OnlyIn(Dist.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {
        @Override
        public <S extends LivingEntityRenderState> void align(ItemStack stack, SlotReference reference, EntityModel<S> model, S renderState, PoseStack matrices) {
            if(!(model instanceof HeadedModel headedModel)) return;

            AccessoryRenderer.transformToModelPart(matrices, headedModel.getHead(), null, 1, null);
        }

        @Override
        public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
            align(stack, reference, model, renderState, matrices);
            matrices.scale(2, 2, 2);
            matrices.translate(0, 1/4f, 0);
            for (int i = 0; i < stack.getCount(); i++) {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, reference.entity().level(), 0);
                matrices.translate(0, 1/2f, 0);
            }
        }
    }
}