package io.wispforest.accessories.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoryRegistry;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryRenderOverrideComponent;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
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
            AccessoriesRendererRegistry.registerRenderer(item, () -> BuiltinAccessoryRenderers.ARMOR_RENDERER);
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
        if (stack.has(AccessoriesDataComponents.CUSTOM_RENDERER)) {
            return DataDrivenAccessoryRenderer.INSTANCE;
        }

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

        if(armorRenderOverride) return BuiltinAccessoryRenderers.ARMOR_RENDERER;

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

    public static class DataDrivenAccessoryRenderer implements AccessoryRenderer {

        public static final DataDrivenAccessoryRenderer INSTANCE = new DataDrivenAccessoryRenderer();

        @Override
        public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            var data = stack.get(AccessoriesDataComponents.CUSTOM_RENDERER);
            var targetEntity = reference.entity();

            ClientRenderingUtils.handle(data.renderingFunctions(), null, reference.entity(), model, matrices, multiBufferSource, partialTicks,15728880, OverlayTexture.NO_OVERLAY, -1);
        }

        @Override
        public <M extends LivingEntity> void renderOnFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference, PoseStack matrices, EntityModel<M> model, MultiBufferSource multiBufferSource, int light) {
            var data = stack.get(AccessoriesDataComponents.CUSTOM_RENDERER);
            var targetEntity = reference.entity();

            var tickRateManager = targetEntity.level().tickRateManager();

            var partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(targetEntity));

            ClientRenderingUtils.handle(data.renderingFunctions(), arm, reference.entity(), model, matrices, multiBufferSource, partialTicks,15728880, OverlayTexture.NO_OVERLAY, -1);
        }


        // TODO: ATTEMPT TO DEAL WITH ALWAYS RENDERING BY CHECKING THE TREE OF FUNCTIONS TO SEE IF SUCH EXISTS INSTAED OF ALWAYS TRUE
        @Override
        public boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
            return true;
        }
    }
}