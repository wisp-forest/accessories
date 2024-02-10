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
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PotatoAccessory implements Accessory {

    @Environment(EnvType.CLIENT)
    public static void clientInit(){
        AccessoriesRendererRegistery.registerRenderer(Items.POTATO, new PotatoAccessory.Renderer());
    }

    public static void init(){
        AccessoriesAPI.registerAccessory(Items.POTATO, new PotatoAccessory());
    }

    @Environment(EnvType.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {

        @Override
        public <M extends LivingEntity> void align(ItemStack stack, SlotReference reference, EntityModel<M> model, PoseStack matrices) {
            if(!(model instanceof HumanoidModel<? extends LivingEntity> humanoidModel)) return;

            AccessoryRenderer.transformToModelPart(matrices, humanoidModel.body, 0, 0, -1);
        }
    }
}