package io.wispforest.accessories.api.client;

import com.google.common.base.Suppliers;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesStorage;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.SimpleAccessoriesStorage;
import io.wispforest.accessories.api.slot.SlotPath;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AccessoriesRenderStateKeys {
    public static final ContextKey<SlotPath> SLOT_PATH = new ContextKey<>(Accessories.of("slot_path"));
    public static final ContextKey<ItemStack> ITEM_STACK = new ContextKey<>(Accessories.of("item_stack"));
    public static final ContextKey<@Nullable ItemStackRenderState> ITEM_STACK_STATE = new ContextKey<>(Accessories.of("item_stack_state"));
    public static final ContextKey<LivingEntityRenderState> ENTITY_STATE = new ContextKey<>(Accessories.of("entity_state"));

    public static final ContextKey<@Nullable Map<SlotPath, AccessoryRenderState>> ACCESSORY_RENDER_STATES = new ContextKey<>(Accessories.of("item_stack_render_state"));
    public static final ContextKey<@Nullable AccessoriesStorageLookup> STORAGE_LOOKUP = new ContextKey<>(Accessories.of("storage_lookup"));

    public static final DefaultedContextKey<List<@Nullable AccessoryRenderState>> NESTED_ACCESSORY_RENDER_STATES = new DefaultedContextKey<>(Accessories.of("item_stack_render_state"), List::of);

    public static final ContextKey<@Nullable CameraRenderState> CAMERA_STATE = new ContextKey<>(Accessories.of("entity_uuid"));

    public static final DefaultedContextKey<UUID> ENTITY_UUID = new DefaultedContextKey<>(Accessories.of("entity_uuid"), Suppliers.memoize(UUID::randomUUID));
    public static final ContextKey<Integer> ENTITY_ID = new DefaultedContextKey<>(Accessories.of("entity_id"), () -> 0);

    public static final DefaultedContextKey<Float> PARTIAL_TICKS = new DefaultedContextKey<>(Accessories.of("partial_ticks"), () -> 0f);
    public static final DefaultedContextKey<Integer> LIGHT = new DefaultedContextKey<>(Accessories.of("light"), () -> 1);

    public static final ContextKey<HumanoidArm> ARM = new ContextKey<>(Accessories.of("arm"));


    public static void setupStateForAccessories(EntityRenderState state, Entity entity, float partialTick, HumanoidArm arm) {
        if (state instanceof LivingEntityRenderState livingState) livingState.setStateData(ARM, arm);

        setupStateForAccessories(state, entity, partialTick);
    }

    public static void setupStateForAccessories(EntityRenderState state, Entity entity, float partialTick) {
        if (!(state instanceof LivingEntityRenderState livingState) || !(entity instanceof LivingEntity livingEntity)) return;

        livingState.setStateData(AccessoriesRenderStateKeys.ENTITY_UUID, livingEntity.getUUID());
        livingState.setStateData(AccessoriesRenderStateKeys.ENTITY_ID, livingEntity.getId());

        livingState.setStateData(AccessoriesRenderStateKeys.PARTIAL_TICKS, partialTick);

        var capability = livingEntity.accessoriesCapability();

        if (capability == null) return;

        var map = new LinkedHashMap<String, AccessoriesStorage>();

        for (var entry : capability.getContainers().entrySet()) {
            map.put(entry.getKey(), SimpleAccessoriesStorage.copy(entry.getValue()));
        }

        if (map.isEmpty()) return;

        AccessoriesStorageLookup lookup = () -> map;

        livingState.setStateData(AccessoriesRenderStateKeys.STORAGE_LOOKUP, lookup);

        var renderStates = new LinkedHashMap<SlotPath, AccessoryRenderState>();

        for (var container : lookup.getContainers().values()) {
            var accessories = container.getAccessories();
            var cosmetics = container.getCosmeticAccessories();

            for (int i = 0; i < container.getSize(); i++) {
                var stack = accessories.getItem(i);
                var cosmeticStack = cosmetics.getItem(i);

                if (!cosmeticStack.isEmpty() && Accessories.config().clientOptions.showCosmeticAccessories()) {
                    stack = cosmeticStack;
                }

                if (stack.isEmpty()) continue;

                //--

                var renderer = AccessoriesRendererRegistry.getRenderer(stack);

                if (renderer.isEmpty()) continue;

                var path = container.createPath(i);

                var accessoryState = renderer.createRenderState(stack, path, lookup, livingEntity, livingState);

                if (accessoryState != null) renderStates.put(path, accessoryState);
            }
        }

        livingState.setStateData(AccessoriesRenderStateKeys.ACCESSORY_RENDER_STATES, renderStates);
    }
}
