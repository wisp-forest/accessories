package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesLoaderInternals;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoryRegistry;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryRenderOverrideComponent;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.compat.GeckoLibCompat;
import io.wispforest.accessories.mixin.client.HumanoidArmorLayerAccessor;
import io.wispforest.accessories.mixin.client.LivingEntityRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Main class used to register and hold {@link AccessoryRenderer}'s. This contains a method to
 * reload all renders when a data reload occurs for the client combined with method to retrieve renders.
 */
public class AccessoriesRendererRegistry {

    private static final Map<Item, Supplier<AccessoryRenderer>> RENDERERS = new HashMap<>();

    private static final Map<Item, AccessoryRenderer> CACHED_RENDERERS = new HashMap<>();

    /**
     * Main method used to register an {@link Item} with a given {@link AccessoryRenderer}
     */
    public static void registerRenderer(Item item, Supplier<AccessoryRenderer> renderer){
        RENDERERS.put(item, renderer);
    }

    /**
     * Method used to prevent default rendering for the given {@link Item}
     * <br/>
     * This should ONLY be used if ABSOLUTELY necessary
     */
    public static void registerNoRenderer(Item item){
        RENDERERS.put(item, () -> null);
    }

    /**
     * Registers the given item as if it should render like armor piece equipped within the targeted slot
     * as dictated by {@link Equippable#slot()}
     */
    public static void registerArmorRendering(Item item) {
        if (!AccessoriesRendererRegistry.hasRenderer(item)) {
            AccessoriesRendererRegistry.registerRenderer(item, () -> ARMOR_RENDERER);
        }
    }

    public static boolean hasRenderer(Item item) {
        return RENDERERS.containsKey(item);
    }

    //--

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    @Nullable
    public static AccessoryRenderer getRender(ItemStack stack){
        var renderOverrides = stack.getOrDefault(AccessoriesDataComponents.RENDER_OVERRIDE, AccessoryRenderOverrideComponent.DEFAULT);

        var defaultRenderOverride = renderOverrides.defaultRenderOverride();

        if(defaultRenderOverride != null) {
            if(defaultRenderOverride) {
                return DefaultAccessoryRenderer.INSTANCE;
            } else if(AccessoryRegistry.isDefaultAccessory(stack)) {
                return null;
            }
        }

        var armorRenderOverride = renderOverrides.useArmorRenderer();

        if(armorRenderOverride) return ARMOR_RENDERER;

        return getRender(stack.getItem());
    }

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    @Nullable
    public static AccessoryRenderer getRender(Item item){
        var renderer = CACHED_RENDERERS.getOrDefault(item, DefaultAccessoryRenderer.INSTANCE);

        if(renderer == null && Accessories.config().clientOptions.forceNullRenderReplacement()) {
            renderer = DefaultAccessoryRenderer.INSTANCE;
        }

        return renderer;
    }

    public static void onReload() {
        CACHED_RENDERERS.clear();

        RENDERERS.forEach((item, supplier) -> CACHED_RENDERERS.put(item, supplier.get()));
    }

    private static final AccessoryRenderer ARMOR_RENDERER = new AccessoryRenderer() {
        @Override
        public <STATE extends LivingEntityRenderState> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<STATE> model, STATE renderState, MultiBufferSource multiBufferSource, int light, float partialTicks) {
            var entityRender = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(reference.entity());

            if (!(entityRender instanceof LivingEntityRendererAccessor<?, ?, ?> accessor)) return;
            if (!(stack.has(DataComponents.EQUIPPABLE))) return;
            if (!(renderState instanceof HumanoidRenderState humanoidRenderState)) return;

            var equipmentSlot = stack.get(DataComponents.EQUIPPABLE).slot();

            var possibleLayer = accessor.getLayers().stream()
                    .filter(renderLayer -> renderLayer instanceof HumanoidArmorLayer<?,?,?>)
                    .findFirst();

            possibleLayer.ifPresent(layer -> {
                rendererArmor((HumanoidArmorLayer<HumanoidRenderState,?,?>) layer, stack, matrices, multiBufferSource, humanoidRenderState, equipmentSlot, light, partialTicks);
            });
        }
    };

    private static <S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> void rendererArmor(HumanoidArmorLayer<S, M, A> armorLayer, ItemStack stack, PoseStack poseStack, MultiBufferSource multiBufferSource, S renderState, EquipmentSlot equipmentSlot, int light, float partialTicks) {
        var armorLayerAccessor = (HumanoidArmorLayerAccessor<S, A>) armorLayer;

        var armorModel = armorLayerAccessor.accessories$getArmorModel(renderState, equipmentSlot);

        if (!attemptGeckoRender(stack, poseStack, multiBufferSource, renderState, equipmentSlot, light, partialTicks, armorLayer.getParentModel(), armorModel, armorLayerAccessor::accessories$setPartVisibility)) {
            armorLayerAccessor.accessories$renderArmorPiece(poseStack, multiBufferSource, stack, equipmentSlot, light, armorModel);
        }
    }

    private static <S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> boolean attemptGeckoRender(ItemStack stack, PoseStack poseStack, MultiBufferSource multiBufferSource, S renderState, EquipmentSlot equipmentSlot, int light, float partialTicks, M parentModel, A armorModel, BiConsumer<A, EquipmentSlot> partVisibilitySetter) {
        if (!AccessoriesLoaderInternals.isModLoaded("geckolib")) return false;

        return GeckoLibCompat.renderGeckoArmor(poseStack, multiBufferSource, renderState, stack, equipmentSlot, parentModel, armorModel, partialTicks, light, partVisibilitySetter);
    }

}