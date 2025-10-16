package io.wispforest.accessories.api.client;

import io.wispforest.accessories.api.slot.SlotPath;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AccessoryRenderState implements RenderStateStorage {
    private final List<RenderStateStorage> otherStates = new ArrayList<>();
    private final Reference2ObjectMap<ContextKey<?>, Object> stateData = new Reference2ObjectOpenHashMap<>();

    @Override
    public <T> T getStateData(ContextKey<T> key) {
        if (this.stateData.containsKey(key)) return (T) this.stateData.get(key);

        for (var otherState : otherStates) {
            if (otherState.hasStateData(key)) return getStateData(key);
        }

        return key instanceof DefaultedContextKey<T> defaultedKey
            ? defaultedKey.getDefaultValue()
            : null;
    }

    @Override
    public <T> boolean hasStateData(ContextKey<T> key) {
        if (this.stateData.containsKey(key)) return true;

        return otherStates.stream().anyMatch(state -> state.hasStateData(key));
    }

    @Override
    public <T> void setStateData(ContextKey<T> key, T data) {
        if (data == null) {
            this.stateData.remove(key);
        } else {
            this.stateData.put(key, data);
        }
    }

    @Override
    public void clearExtraData() {
        this.stateData.clear();
    }

    public static AccessoryRenderState setupState(SlotPath path, ItemStack stack, LivingEntity entity, LivingEntityRenderState entityState, boolean createStackRenderState) {
        var state = new AccessoryRenderState();

        state.setStateData(AccessoriesRenderStateKeys.SLOT_PATH, path);
        state.setStateData(AccessoriesRenderStateKeys.ITEM_STACK, stack);
        state.setStateData(AccessoriesRenderStateKeys.ENTITY_STATE, entityState);

        if (createStackRenderState) {
            var stackRenderState = new ItemStackRenderState();

            Minecraft.getInstance().getItemModelResolver().updateForLiving(stackRenderState, stack, ItemDisplayContext.FIXED, entity);

            state.setStateData(AccessoriesRenderStateKeys.ITEM_STACK_STATE, stackRenderState);
        }

        return state;
    }
}
