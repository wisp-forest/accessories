package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.slot.SlotPath;
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
    public <S extends LivingEntityRenderState> void render(ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
        delegate.render(stack, path, matrices, model, renderState, multiBufferSource, light, partialTicks);
    }

    @Override
    public boolean shouldRender(boolean isRendering) {
        return delegate.shouldRender(isRendering);
    }

    @Override
    public <S extends LivingEntityRenderState> boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotPath path, S renderState) {
        return delegate.shouldRenderInFirstPerson(arm, stack, path, renderState);
    }

    @Override
    public <S extends LivingEntityRenderState> void renderOnFirstPerson(HumanoidArm arm, ItemStack stack, SlotPath path, PoseStack matrices, EntityModel<S> model, S renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
        delegate.renderOnFirstPerson(arm, stack, path, matrices, model, renderState, multiBufferSource, light, partialTicks);
    }
}
