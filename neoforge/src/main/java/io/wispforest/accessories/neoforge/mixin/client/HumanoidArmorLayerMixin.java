package io.wispforest.accessories.neoforge.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.AccessoriesLoaderInternals;
import io.wispforest.accessories.api.client.ArmorRenderingExtension;
import io.wispforest.accessories.compat.GeckoLibCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> implements ArmorRenderingExtension<T> {

    public HumanoidArmorLayerMixin(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Shadow
    protected abstract void renderArmorPiece(PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int i, A humanoidModel, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch);

    @Shadow
    private A getArmorModel(EquipmentSlot slot) { return null; }

    @Shadow protected abstract void setPartVisibility(A humanoidModel, EquipmentSlot equipmentSlot);

    @Unique
    @Nullable
    private ItemStack tempStack = null;

    @Override
    public void renderEquipmentStack(ItemStack stack, PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.tempStack = stack;

        this.renderArmorPiece(poseStack, multiBufferSource, livingEntity, equipmentSlot, light, this.getArmorModel(equipmentSlot), limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);

        this.tempStack = null;
    }

    @WrapOperation(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack getAlternativeStack(LivingEntity instance, EquipmentSlot equipmentSlot, Operation<ItemStack> original) {
        if (tempStack != null) return tempStack;

        return original.call(instance, equipmentSlot);
    }

    @Unique
    private boolean attemptGeckoRender(ItemStack stack, PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!AccessoriesLoaderInternals.isModLoaded("geckolib")) return false;

        return GeckoLibCompat.renderGeckoArmor(poseStack, multiBufferSource, livingEntity, stack, equipmentSlot, this.getParentModel(), this.getArmorModel(equipmentSlot), partialTicks, light, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, this::setPartVisibility);
    }
}
