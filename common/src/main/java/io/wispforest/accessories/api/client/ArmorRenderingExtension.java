package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.mixin.client.LivingEntityRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

public interface ArmorRenderingExtension<T extends LivingEntity> {

    AccessoryRenderer RENDERER = new AccessoryRenderer() {
        @Override
        public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var entityRender = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(reference.entity());

            if (!(entityRender instanceof LivingEntityRendererAccessor<?, ?> accessor)) return;

            if (!(stack.getItem() instanceof Equipable equipable)) return;

            var equipmentSlot = equipable.getEquipmentSlot();

            var possibleLayer = accessor.getLayers().stream()
                    .filter(renderLayer -> renderLayer instanceof ArmorRenderingExtension)
                    .findFirst();

            possibleLayer.ifPresent(layer -> ((ArmorRenderingExtension) layer).renderEquipmentStack(stack, matrices, multiBufferSource, reference.entity(), equipmentSlot, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch));
        }
    };

    default void renderEquipmentStack(ItemStack stack, PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        throw new IllegalStateException("Injected interface method is unimplemented!");
    }
}