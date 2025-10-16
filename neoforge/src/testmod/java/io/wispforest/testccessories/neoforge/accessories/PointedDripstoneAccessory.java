package io.wispforest.testccessories.neoforge.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderState;
import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.client.renderers.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.core.Accessory;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.testccessories.neoforge.Testccessories;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PointedDripstoneAccessory implements Accessory {

    private static final ResourceLocation ATTACK_DAMAGE_LOCATION = Testccessories.of("pointed_dripstone_accessory_attack_damage");

    public static void clientInit() {
        AccessoriesRendererRegistry.bindItemToRenderer(Items.POINTED_DRIPSTONE, Testccessories.of("dripped_gloves"), Renderer::new);
    }

    public static void init() {
        AccessoryRegistry.register(Items.POINTED_DRIPSTONE, new PointedDripstoneAccessory());
    }

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        if(reference.slotName().equals("hand") || reference.slotName().equals("hat")) {
            builder.addStackable(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_LOCATION, 3 * (stack.getCount() / 64f), AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static class Renderer implements SimpleAccessoryRenderer {

        @Override
        public boolean shouldRender(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, boolean isRenderingEnabled) {
            var arm = entityState.getStateData(AccessoriesRenderStateKeys.ARM);

            return (arm != null)
                ? path.index() % 2 == 0 ? arm == HumanoidArm.RIGHT : arm == HumanoidArm.LEFT
                : SimpleAccessoryRenderer.super.shouldRender(stack, path, storageLookup, entity, entityState, isRenderingEnabled);
        }

        @Override
        public <S extends LivingEntityRenderState> void renderStack(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices, SubmitNodeCollector collector, ItemStack stack, ItemStackRenderState stackRenderState, int light) {
            for (int i = 0; i < stack.getCount(); i++) {
                if (i > 0) matrices.mulPose(Axis.YP.rotationDegrees(Math.min(90, 360f / stack.getCount())));
                matrices.pushPose();
                matrices.translate(Math.max(0,stack.getCount() - 8) * 0.01, 0, 0);
                stackRenderState.submit(matrices, collector, light, OverlayTexture.NO_OVERLAY, 0);
                matrices.popPose();
            }
        }

        @Override
        public <S extends LivingEntityRenderState> void align(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices) {
            if (!(model instanceof HumanoidModel<? extends HumanoidRenderState> humanoidModel)) return;

            var path = accessoryState.getStateData(AccessoriesRenderStateKeys.SLOT_PATH);

            var armModelPart = (path.index() % 2 == 0) ? humanoidModel.rightArm : humanoidModel.leftArm;

            AccessoryRenderer.transformToModelPart(matrices, armModelPart, 0, -1, 0);

            matrices.translate(0, -0.5, 0);
        }
    }
}