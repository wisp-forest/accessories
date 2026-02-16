package io.wispforest.accessories.api.client.renderers;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesLoaderInternals;
import io.wispforest.accessories.api.AccessoriesStorageLookup;
import io.wispforest.accessories.api.client.AccessoriesRenderStateKeys;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderState;
import io.wispforest.accessories.api.client.rendering.RenderingFunctionOps;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.compat.GeckoLibCompat;
import io.wispforest.accessories.mixin.client.HumanoidArmorLayerAccessor;
import io.wispforest.accessories.mixin.client.LivingEntityRendererAccessor;
import io.wispforest.accessories.pond.WingsLayerExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public class BuiltinAccessoryRenderers {

    public static final AccessoryRenderer ARMOR_RENDERER = new AccessoryRenderer() {
        @Override
        public <S extends LivingEntityRenderState> void render(
            AccessoryRenderState accessoryState,
            S entityState,
            EntityModel<S> model,
            PoseStack matrices,
            SubmitNodeCollector collector
        ) {
            if (!(entityState instanceof HumanoidRenderState humanoidRenderState)) return;

            var entityRender = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entityState);

            if (!(entityRender instanceof LivingEntityRendererAccessor<?, ?, ?> accessor)) return;

            var stack = accessoryState.getStateData(AccessoriesRenderStateKeys.ITEM_STACK);

            if (!(stack.has(DataComponents.EQUIPPABLE))) return;

            var equipmentSlot = stack.get(DataComponents.EQUIPPABLE).slot();

            var possibleLayer = accessor.accessories$getLayers().stream()
                    .filter(renderLayer -> renderLayer instanceof HumanoidArmorLayer<?,?,?>)
                    .findFirst();

            possibleLayer.ifPresent(layer -> {
                rendererArmor((HumanoidArmorLayer<HumanoidRenderState,?,?>) layer, stack, matrices, collector, humanoidRenderState, equipmentSlot);
            });
        }
    };

    private static <S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> void rendererArmor(HumanoidArmorLayer<S, M, A> armorLayer, ItemStack stack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, S renderState, EquipmentSlot equipmentSlot) {
        var armorLayerAccessor = (HumanoidArmorLayerAccessor<S, A>) armorLayer;

        var light = renderState.getStateData(AccessoriesRenderStateKeys.LIGHT);
        var partialTicks = renderState.getStateData(AccessoriesRenderStateKeys.PARTIAL_TICKS);

        if (!attemptGeckoRender(stack, poseStack, submitNodeCollector, renderState, equipmentSlot, light, partialTicks, armorLayer.getParentModel())) {
            armorLayerAccessor.accessories$renderArmorPiece(poseStack, submitNodeCollector, stack, equipmentSlot, light, renderState);
        }
    }

    private static <S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> boolean attemptGeckoRender(ItemStack stack, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, S renderState, EquipmentSlot equipmentSlot, int light, float partialTicks, M parentModel) {
        if (!AccessoriesLoaderInternals.INSTANCE.isModLoaded("geckolib")) return false;

        return GeckoLibCompat.renderGeckoArmor(poseStack, submitNodeCollector, renderState, stack, equipmentSlot, parentModel, partialTicks, light);
    }

    //--

    public static final AccessoryRenderer ELYTRA_RENDERER = new AccessoryRenderer() {
        @Override
        public <S extends LivingEntityRenderState> void render(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices, SubmitNodeCollector collector) {
            if (!(entityState instanceof HumanoidRenderState humanoidRenderState)) return;

            var entityRender = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entityState);

            if (!(entityRender instanceof LivingEntityRendererAccessor<?, ?, ?> accessor)) return;

            var stack = accessoryState.getStateData(AccessoriesRenderStateKeys.ITEM_STACK);

            if (!(stack.has(DataComponents.GLIDER))) return;

            var possibleLayer = accessor.accessories$getLayers().stream()
                    .filter(renderLayer -> renderLayer instanceof WingsLayer<?,?>)
                    .findFirst();

            var light = entityState.getStateData(AccessoriesRenderStateKeys.LIGHT);

            possibleLayer.ifPresent(layer -> ((WingsLayerExtension<HumanoidRenderState>) layer).renderStack(stack, matrices, collector, light, humanoidRenderState));
        }
    };

    public static final DataDrivenAccessoryRenderer DATA_DRIVEN = new DataDrivenAccessoryRenderer();

    @ApiStatus.Internal
    public static class DataDrivenAccessoryRenderer implements AccessoryRenderer {

        @Override
        public <S extends LivingEntityRenderState> void render(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices, SubmitNodeCollector collector) {
            var stack = accessoryState.getStateData(AccessoriesRenderStateKeys.ITEM_STACK);
            var path = accessoryState.getStateData(AccessoriesRenderStateKeys.SLOT_PATH);

            var data = stack.get(AccessoriesDataComponents.CUSTOM_RENDERER);

            if (data == null || data.renderingFunctions() == null) return;

            var light = entityState.getStateData(AccessoriesRenderStateKeys.LIGHT);
            var partialTicks = entityState.getStateData(AccessoriesRenderStateKeys.PARTIAL_TICKS);

            RenderingFunctionOps.handleFunctions(stack, path, matrices, model, entityState, collector, light, partialTicks, entityState.getStateData(AccessoriesRenderStateKeys.ARM), 15728880, OverlayTexture.NO_OVERLAY, -1, data.renderingFunctions());
        }

        @Override
        public boolean shouldRender(ItemStack stack, SlotPath path, AccessoriesStorageLookup storageLookup, LivingEntity entity, LivingEntityRenderState entityState, boolean isRenderingEnabled) {
            var data = stack.get(AccessoriesDataComponents.CUSTOM_RENDERER);

            if (data == null || data.renderingFunctions() == null) return false;

            return RenderingFunctionOps.shouldRender(stack, path, storageLookup, entity, entityState, data.renderingFunctions()) && isRenderingEnabled;
        }
    }

    @ApiStatus.Internal
    public static class BundleAccessoryRenderer implements AccessoryNestRenderer {
        @Override
        public List<ItemStack> getInnerStacks(ItemStack holderStack) {
            var contents = holderStack.get(DataComponents.BUNDLE_CONTENTS);

            var items = contents.items();

            return (items instanceof List<ItemStack> stacks) ? stacks : Streams.stream(items).toList();
        }
    }

    public static final ResourceLocation BUNDLE_RENDERER_ID = Accessories.of("bundle_renderer");
    public static final ResourceLocation DEFAULT_RENDERER_ID = Accessories.of("default_renderer");
    public static final ResourceLocation ARMOR_RENDERER_ID = Accessories.of("armor_renderer");
    public static final ResourceLocation ELYTRA_RENDERER_ID = Accessories.of("elytra_renderer");

    static {
        AccessoriesRendererRegistry.bindItemToRenderer(Items.BUNDLE, BUNDLE_RENDERER_ID, BundleAccessoryRenderer::new);
        AccessoriesRendererRegistry.registerRenderer(DEFAULT_RENDERER_ID, () -> DefaultAccessoryRenderer.INSTANCE);
        AccessoriesRendererRegistry.registerRenderer(ARMOR_RENDERER_ID, () -> ARMOR_RENDERER);
        AccessoriesRendererRegistry.registerRenderer(ELYTRA_RENDERER_ID, () -> ELYTRA_RENDERER);
    }

    public static void onAddCallback(Item item) {
//        if (item.getDefaultInstance().has(DataComponents.GLIDER)) {
//            AccessoriesRendererRegistry.registerRenderer(item, () -> ELYTRA_RENDERER);
//        }
    }

    public static final class EmptyRenderer implements AccessoryRenderer {
        @Override public <S extends LivingEntityRenderState> void render(AccessoryRenderState accessoryState, S entityState, EntityModel<S> model, PoseStack matrices, SubmitNodeCollector collector) {}
    }
}
