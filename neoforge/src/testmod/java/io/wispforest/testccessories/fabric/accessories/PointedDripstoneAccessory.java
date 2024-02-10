package io.wispforest.testccessories.fabric.accessories;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistery;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
        AccessoriesRendererRegistery.registerRenderer(Items.POINTED_DRIPSTONE, new PointedDripstoneAccessory.Renderer());
    }

    public static void init() {
        AccessoriesAPI.registerAccessory(Items.POINTED_DRIPSTONE, new PointedDripstoneAccessory());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getModifiers(ItemStack stack, SlotReference reference, UUID uuid) {
        var modifiers = Accessory.super.getModifiers(stack, reference, uuid);
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid, "Pointed Dripstone Accessory Attack Damage", 2, AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Renderer implements AccessoryRenderer {

        public <T extends LivingEntity, M extends EntityModel<T>> void align(LivingEntity entity, M model, PoseStack matrices, int slotIndex) {
            if (!(model instanceof HumanoidModel<? extends LivingEntity> humanoidModel)) return;

            if (slotIndex % 2 == 0)
                AccessoryRenderer.transformToModelPart(matrices, humanoidModel.rightArm, 0, -1, 0);
            else
                AccessoryRenderer.transformToModelPart(matrices, humanoidModel.leftArm, 0, -1, 0);

            matrices.translate(0, -0.5, 0);
        }

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(boolean isRendering, ItemStack stack, SlotReference reference, PoseStack matrices, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float netHeadYaw, float headPitch) {
            if (!isRendering) return;

            align(reference.entity(), renderLayerParent.getModel(), matrices, reference.slot());

            for (int i = 0; i < stack.getCount(); i++) {
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, reference.entity().level(), 0);
                matrices.mulPose(Axis.YP.rotationDegrees(Math.min(90, 360f/stack.getCount())));
            }
        }
    }
}