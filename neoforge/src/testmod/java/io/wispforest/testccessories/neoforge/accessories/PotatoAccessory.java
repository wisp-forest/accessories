package io.wispforest.testccessories.neoforge.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderState;
import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.client.renderers.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.testccessories.neoforge.Testccessories;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.Items;

public class PotatoAccessory implements Accessory {

    public static void clientInit(){
        AccessoriesRendererRegistry.bindItemToRenderer(Items.POTATO, Testccessories.of("potato"), Renderer::new);
    }

    public static void init(){
        AccessoryRegistry.register(Items.POTATO, new PotatoAccessory());
    }

    public static class Renderer implements SimpleAccessoryRenderer {
        @Override
        public <S extends LivingEntityRenderState> void align(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices) {
            if(!(model instanceof HumanoidModel<? extends HumanoidRenderState> humanoidModel)) return;

            AccessoryRenderer.transformToModelPart(matrices, humanoidModel.body, 0, 0, -1);
        }
    }
}