package io.wispforest.testccessories.fabric.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoryRegistry;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PotatoAccessory implements Accessory {

    @Environment(EnvType.CLIENT)
    public static void clientInit(){
        AccessoriesRendererRegistry.registerRenderer(Items.POTATO, Renderer::new);
    }

    public static void init(){
        AccessoryRegistry.registerAccessory(Items.POTATO, new PotatoAccessory());
    }

    @Environment(EnvType.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {

        @Override
        public <S extends LivingEntityRenderState> void align(ItemStack stack, SlotReference reference, EntityModel<S> model, S renderState, PoseStack matrices) {
            if(!(model instanceof HumanoidModel<? extends HumanoidRenderState> humanoidModel)) return;

            AccessoryRenderer.transformToModelPart(matrices, humanoidModel.body, 0, 0, -1);
        }
    }
}