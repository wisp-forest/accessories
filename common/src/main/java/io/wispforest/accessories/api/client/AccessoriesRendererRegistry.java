package io.wispforest.accessories.api.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.client.renderers.BuiltinAccessoryRenderers;
import io.wispforest.accessories.api.client.renderers.DefaultAccessoryRenderer;
import io.wispforest.accessories.api.client.renderers.WrappedAccessoryRenderer;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Main class used to register and hold {@link AccessoryRenderer}'s. This contains a method to
 * reload all renders when a data reload occurs for the client combined with method to retrieve renders.
 */
public class AccessoriesRendererRegistry {

    public static final ResourceLocation NO_RENDERER_ID = Accessories.of("no_renderer");

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Item, ResourceLocation> ITEM_TO_RENDERER = new HashMap<>();
    private static final Map<Item, ResourceLocation> DATA_LOADED_ITEM_TO_RENDERER = new HashMap<>();

    private static final Map<ResourceLocation, Supplier<AccessoryRenderer>> RENDERERS = new HashMap<>();

    private static final BiMap<ResourceLocation, AccessoryRenderer> CACHED_RENDERERS = HashBiMap.create();

    /**
     * Binds the given item to use the following renderer as registered though {@link AccessoriesRendererRegistry#registerRenderer(ResourceLocation, Supplier)}
     * @param item
     * @param rendererId
     */
    public static void bindItemToRenderer(Item item, ResourceLocation rendererId) {
        var entry = ITEM_TO_RENDERER.putIfAbsent(item, rendererId);

        if (entry != null) {
            LOGGER.error("Unable to bind Item with the given Register as Item already has binding: [Item: {}, Renderer: {}]", item , rendererId);
        }
    }

    public static void bindItemToRenderer(Item item, ResourceLocation rendererId, Supplier<AccessoryRenderer> renderer) {
        bindItemToRenderer(item, rendererId);

        registerRenderer(rendererId, renderer);
    }

    /**
     * Method used to prevent default rendering for the given {@link Item}
     * <br/>
     * This should ONLY be used if ABSOLUTELY necessary
     */
    public static void bindItemToEmptyRenderer(Item item){
        bindItemToRenderer(item, NO_RENDERER_ID);
    }

    /**
     * Binds the given item to the {@link BuiltinAccessoryRenderers#ARMOR_RENDERER} meaning it will render like
     * any armor piece equipped within the targeted slot as dictated by {@link Equippable#slot()}
     */
    public static void bindItemToArmorRenderer(Item item){
        bindItemToRenderer(item, BuiltinAccessoryRenderers.ARMOR_RENDERER_ID);
    }

    public static void registerRenderer(ResourceLocation location, Supplier<AccessoryRenderer> renderer) {
        RENDERERS.put(location, renderer);
    }

    //--

    public static boolean hasRenderer(Item item) {
        return getBoundRenderer(item) != null;
    }

    public static boolean hasRenderer(ResourceLocation rendererId) {
        return RENDERERS.containsKey(rendererId);
    }

    //--

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    public static AccessoryRenderer getRenderer(ItemStack stack){
        if (stack.has(AccessoriesDataComponents.CUSTOM_RENDERER) && !stack.is(Items.BUNDLE)) {
            var data = stack.get(AccessoriesDataComponents.CUSTOM_RENDERER);

            if (data != null) return BuiltinAccessoryRenderers.DATA_DRIVEN;

            var defaultRenderOverride = data.defaultRenderOverride();

            if(defaultRenderOverride != null) {
                if(defaultRenderOverride) {
                    return DefaultAccessoryRenderer.INSTANCE;
                } else if(AccessoryRegistry.isDefaultAccessory(stack)) {
                    return new BuiltinAccessoryRenderers.EmptyRenderer();
                }
            }
        }

        return getRenderer(stack.getItem());
    }

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    public static AccessoryRenderer getRenderer(Item item){
        AccessoryRenderer renderer;

        var id = getBoundRenderer(item);

        if (id != null) {
            renderer = getRenderer(id);

            if (renderer == null) {
                renderer = DefaultAccessoryRenderer.INSTANCE;
            }
        } else {
            renderer = DefaultAccessoryRenderer.INSTANCE;
        }

        if(renderer instanceof BuiltinAccessoryRenderers.EmptyRenderer && Accessories.config().clientOptions.forceNullRenderReplacement()) {
            renderer = DefaultAccessoryRenderer.INSTANCE;
        } else if (renderer == null) {
            renderer = new BuiltinAccessoryRenderers.EmptyRenderer();
        }

        return renderer == null ? new BuiltinAccessoryRenderers.EmptyRenderer() : renderer;
    }

    @Nullable
    public static AccessoryRenderer getRenderer(ResourceLocation rendererId) {
        if (rendererId.equals(NO_RENDERER_ID)) return new BuiltinAccessoryRenderers.EmptyRenderer();

        return CACHED_RENDERERS.get(rendererId);
    }

    @Nullable
    public static ResourceLocation getRendererId(AccessoryRenderer renderer) {
        return CACHED_RENDERERS.inverse().get(renderer);
    }

    @Nullable
    public static ResourceLocation getBoundRenderer(Item item) {
        if (DATA_LOADED_ITEM_TO_RENDERER.containsKey(item)) {
            return DATA_LOADED_ITEM_TO_RENDERER.get(item);
        }

        return ITEM_TO_RENDERER.get(item);
    }

    //--

    @ApiStatus.Internal
    public static void setDataLoadedItemToRenderer(Map<Item, ResourceLocation> data) {
        DATA_LOADED_ITEM_TO_RENDERER.clear();
        DATA_LOADED_ITEM_TO_RENDERER.putAll(data);
    }

    @ApiStatus.Internal
    public static void onReload() {
        CACHED_RENDERERS.clear();

        RENDERERS.forEach((rendererId, supplier) -> {
            var renderer = supplier.get();

            if (renderer == null) {
                LOGGER.warn("A given renderer [{}] was found to be returning a null renderer which is not advised as method to indicate no rendering!", rendererId);

                renderer = new BuiltinAccessoryRenderers.EmptyRenderer();
            }

            var otherRendererId = CACHED_RENDERERS.inverse().get(renderer);

            if (otherRendererId != null) {
                LOGGER.warn("A given renderer [{}] was found to be shared by another renderer [{}], such will be wrapped to prevent crashing and should be reported!", rendererId, otherRendererId);

                renderer = new WrappedAccessoryRenderer(renderer);
            }

            CACHED_RENDERERS.put(rendererId, renderer);
        });
    }

    //--

    @Deprecated(forRemoval = true)
    public static ResourceLocation getRendererId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    /**
     * Main method used to register an {@link Item} with a given {@link AccessoryRenderer}
     */
    @Deprecated(forRemoval = true)
    public static void registerRenderer(Item item, Supplier<@NotNull AccessoryRenderer> renderer){
        var rendererId = getRendererId(item);

        registerRenderer(rendererId, renderer);
        bindItemToRenderer(item, rendererId);
    }

    /**
     * Method used to prevent default rendering for the given {@link Item}
     * <br/>
     * This should ONLY be used if ABSOLUTELY necessary
     */
    @Deprecated(forRemoval = true)
    public static void registerNoRenderer(Item item){
        bindItemToRenderer(item, NO_RENDERER_ID);
    }


    @Deprecated(forRemoval = true)
    public static void registerArmorRendering(Item item) {
        if (!AccessoriesRendererRegistry.hasRenderer(item)) {
            var rendererId = getRendererId(item);

            AccessoriesRendererRegistry.registerRenderer(rendererId, () -> BuiltinAccessoryRenderers.ARMOR_RENDERER);
            AccessoriesRendererRegistry.bindItemToRenderer(item, rendererId);
        }
    }
}