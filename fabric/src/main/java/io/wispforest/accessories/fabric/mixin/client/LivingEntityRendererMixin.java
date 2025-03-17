package io.wispforest.accessories.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.pond.CosmeticArmorLookupTogglable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LivingEntityRenderer.class, remap = false)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @WrapMethod(method = {
            "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            "method_4054(Lnet/minecraft/class_1309;FFLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V" //TODO: WHY DO I NEEDED THIS!
    }, remap = false, expect = 1, require = 1, allow = 1)
    private void accessories$adjustArmorLookup(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, Operation<Void> original) {
        ((CosmeticArmorLookupTogglable)entity).setLookupToggle(true);

        original.call(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        ((CosmeticArmorLookupTogglable)entity).setLookupToggle(false);
    }
}
