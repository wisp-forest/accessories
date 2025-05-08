package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class WrappedAccessoryRenderer implements AccessoryRenderer {

    private final AccessoryRenderer delegate;

    public WrappedAccessoryRenderer(AccessoryRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
        delegate.render(stack, reference, matrices, model, renderState, multiBufferSource, light, partialTicks);
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
    public <S extends LivingEntityRenderState> void renderOnFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
        delegate.renderOnFirstPerson(arm, stack, reference, matrices, model, renderState, multiBufferSource, light, partialTicks);
    }
}
