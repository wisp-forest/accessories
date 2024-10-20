package io.wispforest.testccessories.fabric.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.fabric.Testccessories;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PointedDripstoneAccessory implements Accessory {

    private static final ResourceLocation ATTACK_DAMAGE_LOCATION = Testccessories.of("pointed_dripstone_accessory_attack_damage");

    @Environment(EnvType.CLIENT)
    public static void clientInit() {
        AccessoriesRendererRegistry.registerRenderer(Items.POINTED_DRIPSTONE, Renderer::new);
    }

    public static void init() {
        AccessoriesAPI.registerAccessory(Items.POINTED_DRIPSTONE, new PointedDripstoneAccessory());
    }

    @Override
    public void getModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        if(reference.slotName().equals("hand") || reference.slotName().equals("hat")) {
            builder.addStackable(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_LOCATION, 3 * (stack.getCount() / 64f), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Renderer implements SimpleAccessoryRenderer {

        public <M extends LivingEntity> void align(ItemStack stack, SlotReference reference, EntityModel<M> model, PoseStack matrices) {
            if (!(model instanceof HumanoidModel<? extends LivingEntity> humanoidModel)) return;

            var armModelPart = (reference.slot() % 2 == 0) ? humanoidModel.rightArm : humanoidModel.leftArm;

            AccessoryRenderer.transformToModelPart(matrices, armModelPart, 0, -1, 0);

            matrices.translate(0, -0.5, 0);
        }

        @Override
        public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            align(stack, reference, model, matrices);

            for (int i = 0; i < stack.getCount(); i++) {
                if (i > 0) matrices.mulPose(Axis.YP.rotationDegrees(Math.min(90, 360f / stack.getCount())));
                matrices.pushPose();
                matrices.translate(Math.max(0,stack.getCount() - 8)*0.01, 0, 0);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, matrices, multiBufferSource, reference.entity().level(), 0);
                matrices.popPose();
            }
        }

        @Override
        public boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
            return reference.slot() % 2 == 0 ? arm == HumanoidArm.RIGHT : arm == HumanoidArm.LEFT;
        }
    }
}