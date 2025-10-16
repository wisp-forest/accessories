package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoryRenderState;
import io.wispforest.accessories.api.slot.SlotPath;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class WrappedAccessoryRenderer implements AccessoryRenderer {

    private final AccessoryRenderer delegate;

    public WrappedAccessoryRenderer(AccessoryRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public <S extends LivingEntityRenderState> void render(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices, SubmitNodeCollector collector) {
        this.delegate.render(accessoryState, entityState, model, matrices, collector);
    }

    @Override
    public AccessoryRenderState createRenderState(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState renderState) {
        return this.delegate.createRenderState(stack, path, storageLookup, entity, renderState);
    }

    @Override
    public void extractRenderState(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, AccessoryRenderState accessoryState) {
        this.delegate.extractRenderState(stack, path, storageLookup, entity, entityState, accessoryState);
    }

    @Override
    public boolean shouldCreateStackRenderState() {
        return this.delegate.shouldCreateStackRenderState();
    }

    @Override
    public boolean shouldRender(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState renderState, boolean isRendering) {
        return this.delegate.shouldRender(stack, path, storageLookup, entity, renderState, isRendering);
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }
}
