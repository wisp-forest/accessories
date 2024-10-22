package io.wispforest.accessories.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccessor<S extends HumanoidRenderState, A extends HumanoidModel<S>> {
    @Invoker("renderArmorPiece")
    void accessories$renderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, ItemStack itemStack, EquipmentSlot equipmentSlot, int i, A humanoidModel);

    @Invoker("getArmorModel")
    A accessories$getArmorModel(S humanoidRenderState, EquipmentSlot equipmentSlot);

    @Invoker("setPartVisibility")
    void accessories$setPartVisibility(A model, EquipmentSlot slot);
}
