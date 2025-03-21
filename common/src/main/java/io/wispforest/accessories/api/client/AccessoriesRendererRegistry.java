package io.wispforest.accessories.api.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryRenderOverrideComponent;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Main class used to register and hold {@link AccessoryRenderer}'s. This contains a method to
 * reload all renders when a data reload occurs for the client combined with method to retrieve renders.
 */
public class AccessoriesRendererRegistry {

    private static final BiMap<ResourceLocation, Supplier<AccessoryRenderer>> RENDERERS = HashBiMap.create();

    private static final BiMap<ResourceLocation, AccessoryRenderer> CACHED_RENDERERS = HashBiMap.create();

    public static void registerRenderer(ResourceLocation location, Supplier<AccessoryRenderer> renderer) {
        RENDERERS.put(location, renderer);
    }

    /**
     * Main method used to register an {@link Item} with a given {@link AccessoryRenderer}
     */
    public static void registerRenderer(Item item, Supplier<AccessoryRenderer> renderer){
        registerRenderer(getRendererId(item), renderer);
    }

    /**
     * Method used to prevent default rendering for the given {@link Item}
     * <br/>
     * This should ONLY be used if ABSOLUTELY necessary
     */
    public static void registerNoRenderer(Item item){
        registerRenderer(item, () -> null);
    }

    /**
     * Registers the given item as if it should render like armor piece equipped within the targeted slot
     * as dictated by {@link Equipable#getEquipmentSlot()}
     */
    public static void registerArmorRendering(Item item) {
        if (item instanceof Equipable && !AccessoriesRendererRegistry.hasRenderer(item)) {
            AccessoriesRendererRegistry.registerRenderer(item, () -> ArmorRenderingExtension.RENDERER);
        }
    }

    public static boolean hasRenderer(Item item) {
        return hasRenderer(BuiltInRegistries.ITEM.getKey(item));
    }

    public static boolean hasRenderer(ResourceLocation rendererId) {
        return RENDERERS.containsKey(rendererId);
    }

    //--

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    @Nullable
    public static AccessoryRenderer getRenderer(ItemStack stack){
        if (stack.has(AccessoriesDataComponents.CUSTOM_RENDERER) && !stack.is(Items.BUNDLE)) {
            return DataDrivenAccessoryRenderer.INSTANCE;
        }

        var renderOverrides = stack.getOrDefault(AccessoriesDataComponents.RENDER_OVERRIDE, AccessoryRenderOverrideComponent.DEFAULT);

        var defaultRenderOverride = renderOverrides.defaultRenderOverride();

        if(defaultRenderOverride != null) {
            if(defaultRenderOverride) {
                return DefaultAccessoryRenderer.INSTANCE;
            } else if(AccessoriesAPI.isDefaultAccessory(AccessoriesAPI.getOrDefaultAccessory(stack))) {
                return null;
            }
        }

        var armorRenderOverride = renderOverrides.useArmorRenderer();

        if(armorRenderOverride) return ArmorRenderingExtension.RENDERER;

        return getRenderer(stack.getItem());
    }

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    @Nullable
    public static AccessoryRenderer getRenderer(Item item){
        var id = getRendererId(item);
        var renderer = getRenderer(id);

        if (!CACHED_RENDERERS.containsKey(id)) {
            renderer = DefaultAccessoryRenderer.INSTANCE;
        }

        if(renderer == null && Accessories.config().clientOptions.forceNullRenderReplacement()) {
            renderer = DefaultAccessoryRenderer.INSTANCE;
        }

        return renderer;
    }

    @Nullable
    public static AccessoryRenderer getRenderer(ResourceLocation rendererId) {
        return CACHED_RENDERERS.get(rendererId);
    }

    @Nullable
    public static ResourceLocation getRendererId(AccessoryRenderer renderer) {
        return CACHED_RENDERERS.inverse().get(renderer);
    }

    //--

    public static ResourceLocation getRendererId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    @ApiStatus.Internal
    public static void onReload() {
        CACHED_RENDERERS.clear();

        RENDERERS.forEach((item, supplier) -> CACHED_RENDERERS.put(item, supplier.get()));
    }

    @ApiStatus.Internal
    public static class DataDrivenAccessoryRenderer implements AccessoryRenderer {

        public static final DataDrivenAccessoryRenderer INSTANCE = new DataDrivenAccessoryRenderer();

        @Override
        public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var data = stack.get(AccessoriesDataComponents.CUSTOM_RENDERER);

            if (data == null) return;

            ClientRenderingUtils.handle(stack, reference.entity(), null, model, matrices, multiBufferSource, partialTicks,15728880, OverlayTexture.NO_OVERLAY, -1, data.renderingFunctions());
        }

        @Override
        public <M extends LivingEntity> void renderOnFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light) {
            var data = stack.get(AccessoriesDataComponents.CUSTOM_RENDERER);

            if (data == null) return;

            var targetEntity = reference.entity();

            var tickRateManager = targetEntity.level().tickRateManager();

            var partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(targetEntity));

            ClientRenderingUtils.handle(stack, targetEntity, arm, model, matrices, multiBufferSource, partialTicks,15728880, OverlayTexture.NO_OVERLAY, -1, data.renderingFunctions());
        }


        // TODO: ATTEMPT TO DEAL WITH ALWAYS RENDERING BY CHECKING THE TREE OF FUNCTIONS TO SEE IF SUCH EXISTS INSTAED OF ALWAYS TRUE
        @Override
        public boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
            return true;
        }
    }

    @ApiStatus.Internal
    private static class BundleAccessoryRenderer implements AccessoryRenderer {
        @Override
        public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var contents = stack.get(DataComponents.BUNDLE_CONTENTS);

            if (contents == null) return;

            DataDrivenAccessoryRenderer.INSTANCE.render(stack, reference, matrices, model, multiBufferSource, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);

            if (contents.items() instanceof List<ItemStack> list) {
                for (int i = 0; i < list.size(); i++) {
                    var innerStack = list.get(i);

                    if (innerStack.isEmpty()) continue;

                    var renderer = AccessoriesRendererRegistry.getRenderer(innerStack);

                    if (renderer == null) continue;

                    matrices.pushPose();

                    try {
                        renderer.render(innerStack, AccessoryNestUtils.create(reference, i), matrices, model, multiBufferSource, light, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                    } catch (Throwable e) {
                        throw new IllegalStateException("[BundleAccessoryRenderer] Unable to render a given inner item stack due the following error: ", e);
                    }

                    matrices.popPose();
                }
            }
        }

        @Override
        public <M extends LivingEntity> void renderOnFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light) {
            var contents = stack.get(DataComponents.BUNDLE_CONTENTS);

            if (contents == null) return;

            DataDrivenAccessoryRenderer.INSTANCE.renderOnFirstPerson(arm, stack, reference, matrices, model, multiBufferSource, light);

            if (contents.items() instanceof List<ItemStack> list) {
                for (int i = 0; i < list.size(); i++) {
                    var innerStack = list.get(i);

                    if (innerStack.isEmpty()) continue;

                    var renderer = AccessoriesRendererRegistry.getRenderer(innerStack);

                    var ref = AccessoryNestUtils.create(reference, i);

                    if (renderer == null || !renderer.shouldRenderInFirstPerson(arm, innerStack, ref)) continue;

                    matrices.pushPose();

                    try {
                        renderer.renderOnFirstPerson(arm, innerStack, ref, matrices, model, multiBufferSource, light);
                    } catch (Throwable e) {
                        throw new IllegalStateException("[BundleAccessoryRenderer] Unable to render a given inner item stack due the following error: ", e);
                    }

                    matrices.popPose();
                }
            }
        }
    }

    static {
        AccessoriesRendererRegistry.registerRenderer(Items.BUNDLE, BundleAccessoryRenderer::new);
    }

    /**
     * @deprecated Use {@link #getRenderer(ItemStack)}
     */
    @Deprecated(forRemoval = true)
    public static AccessoryRenderer getRender(ItemStack stack){
        return getRenderer(stack);
    }

    /**
     * @deprecated Use {@link #getRenderer(Item)}
     */
    @Deprecated(forRemoval = true)
    public static AccessoryRenderer getRender(Item item){
        return getRenderer(item);
    }
}