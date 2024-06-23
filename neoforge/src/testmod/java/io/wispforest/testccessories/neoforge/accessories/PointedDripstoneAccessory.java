package io.wispforest.testccessories.neoforge.accessories;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import io.wispforest.testccessories.neoforge.Testccessories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

public class PointedDripstoneAccessory implements Accessory {

    @OnlyIn(Dist.CLIENT)
    public static void clientInit() {
        AccessoriesRendererRegistry.registerRenderer(Items.POINTED_DRIPSTONE, Renderer::new);
    }

    public static void init() {
        AccessoriesAPI.registerAccessory(Items.POINTED_DRIPSTONE, new PointedDripstoneAccessory());
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, ResourceLocation slotLocation) {
        var modifiers = Accessory.super.getModifiers(stack, reference, slotLocation);
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(Testccessories.of("Pointed Dripstone Accessory Attack Damage"), 2, AttributeModifier.Operation.ADD_VALUE));
        return modifiers;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {

        @Override
        public <M extends LivingEntity> void align(ItemStack stack, SlotReference reference, EntityModel<M> model, PoseStack matrices) {
            if (!(model instanceof HumanoidModel<? extends LivingEntity> humanoidModel)) return;

            if (reference.slot() % 2 == 0)
                AccessoryRenderer.transformToModelPart(matrices, humanoidModel.rightArm, 0, -1, 0);
            else
                AccessoryRenderer.transformToModelPart(matrices, humanoidModel.leftArm, 0, -1, 0);

            matrices.translate(0, -0.5, 0);
        }

        @Override
        public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,  float netHeadYaw, float headPitch) {
            align(stack, reference, model, matrices);

            for (int i = 0; i < stack.getCount(); i++) {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, reference.entity().level(), 0);
                matrices.mulPose(Axis.YP.rotationDegrees(Math.min(90, 360f/stack.getCount())));
            }
        }
    }
}