package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class WrappedAccessoryRenderer implements AccessoryRenderer {

    private final AccessoryRenderer delegate;

    public WrappedAccessoryRenderer(AccessoryRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        delegate.render(stack, reference, matrices, model, multiBufferSource, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public boolean shouldRender(boolean isRendering) {
        return delegate.shouldRender(isRendering);
    }

    @Override
    public boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
        return delegate.shouldRenderInFirstPerson(arm, stack, reference);
    }

    @Override
    public <M extends LivingEntity> void renderOnFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light) {
        delegate.renderOnFirstPerson(arm, stack, reference, matrices, model, multiBufferSource, light);
    }
}
