package io.wispforest.accessories.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.client.ArmorRenderingExtension;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, A extends HumanoidModel<T>> implements ArmorRenderingExtension<T> {

    @Shadow
    protected abstract void renderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int i, A humanoidModel);

    @Shadow
    private A getArmorModel(EquipmentSlot slot) { return null; }

    @Unique
    @Nullable
    private ItemStack tempStack = null;

    @Override
    public void renderEquipmentStack(ItemStack stack, PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.tempStack = stack;

        this.renderArmorPiece(poseStack, multiBufferSource, livingEntity, equipmentSlot, light, this.getArmorModel(equipmentSlot));

        this.tempStack = null;
    }

    @WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack getAlternativeStack(LivingEntity instance, EquipmentSlot equipmentSlot, Operation<ItemStack> original) {
        if (tempStack != null) return tempStack;

        return original.call(instance, equipmentSlot);
    }
}
