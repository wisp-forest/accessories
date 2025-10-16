package io.wispforest.accessories.api.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderState;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.slot.SlotPath;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface AccessoryNestRenderer extends AccessoryRenderer {

    default List<ItemStack> getInnerStacks(ItemStack holderStack) {
        var data = holderStack.get(AccessoriesDataComponents.NESTED_ACCESSORIES);

        if (data == null) return List.of();

        return data.accessories();
    }

    @Override
    default <S extends LivingEntityRenderState> void render(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices, SubmitNodeCollector collector) {
        var innerStates = accessoryState.getStateData(AccessoriesRenderStateKeys.NESTED_ACCESSORY_RENDER_STATES);

        for (var innerState : innerStates) {
            if (innerState == null) continue;

            var innerStack = innerState.getStateData(AccessoriesRenderStateKeys.ITEM_STACK);

            var renderer = AccessoriesRendererRegistry.getRenderer(innerStack);

            matrices.pushPose();

            try {
                renderer.render(innerState, entityState, model, matrices, collector);
            } catch (Exception e) {
                throw new IllegalStateException("[AccessoryNestRenderer] Unable to render a given inner item stack due the following error: ", e);
            } finally {
                matrices.popPose();
            }
        }
    }

    @Override
    default void extractRenderState(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, AccessoryRenderState accessoryState) {
        AccessoryRenderer.super.extractRenderState(stack, path, storageLookup, entity, entityState, accessoryState);

        List<@Nullable AccessoryRenderState> innerStates = new ArrayList<>();

        var innerStacks = getInnerStacks(stack);

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);
            var renderer = AccessoriesRendererRegistry.getRenderer(innerStack);

            var state = renderer.createRenderState(innerStack, SlotPath.withInnerIndex(path, i), storageLookup, entity, entityState);

            innerStates.add(state);
        }

        accessoryState.setStateData(AccessoriesRenderStateKeys.NESTED_ACCESSORY_RENDER_STATES, innerStates);
    }

    @Override
    default boolean shouldRender(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState renderState, boolean isRenderingEnabled) {
        var innerStacks = getInnerStacks(stack);

        for (var innerStack : innerStacks) {
            var renderer = AccessoriesRendererRegistry.getRenderer(innerStack);

            if (renderer.shouldRender(stack, path, storageLookup, entity, renderState, isRenderingEnabled)) {
                return true;
            }
        }

        return AccessoryRenderer.super.shouldRender(stack, path, storageLookup, entity, renderState, isRenderingEnabled);
    }
}
