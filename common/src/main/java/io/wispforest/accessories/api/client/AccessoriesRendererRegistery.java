package io.wispforest.accessories.api.client;

import io.wispforest.accessories.api.AccessoriesAPI;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Main class used to register and hold {@link AccessoryRenderer}'s for the given items
 */
public class AccessoriesRendererRegistery {

    private static final Map<Item, Supplier<AccessoryRenderer>> RENDERERS = new HashMap<>();

    private static final Map<Item, AccessoryRenderer> CACHED_RENDERERS = new HashMap<>();

    /**
     * Main method used to register an {@link Item} with a given {@link AccessoryRenderer}
     */
    public static void registerRenderer(Item item, Supplier<AccessoryRenderer> renderer){
        RENDERERS.put(item, renderer);
    }

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    @Nullable
    public static AccessoryRenderer getRender(Item item){
        var accessory = AccessoriesAPI.getOrDefaultAccessory(item);

        if(accessory == AccessoriesAPI.defaultAccessory()) {
            return DefaultAccessoryRenderer.INSTANCE;
        }

        return CACHED_RENDERERS.get(item);
    }

    public static void onReload() {
        CACHED_RENDERERS.clear();

        RENDERERS.forEach((item, supplier) -> CACHED_RENDERERS.put(item, supplier.get()));
    }
}
