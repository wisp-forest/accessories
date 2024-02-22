package io.wispforest.accessories.api.client;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Main class used to register and hold {@link AccessoryRenderer}'s for the given items
 */
public class AccessoriesRendererRegistery {

    private static final Map<Item, AccessoryRenderer> RENDERERS = new HashMap<>();

    /**
     * Main method used to register an {@link Item} with a given {@link AccessoryRenderer}
     */
    public static void registerRenderer(Item item, AccessoryRenderer renderer){
        RENDERERS.put(item, renderer);
    }

    /**
     * @return An optional representing the {@link AccessoryRenderer} if found within the registry
     */
    public static Optional<AccessoryRenderer> getRender(Item item){
        return Optional.ofNullable(RENDERERS.get(item));
    }

    /**
     * @return Either the {@link AccessoryRenderer} bound to the item or the instance of the {@link DefaultAccessoryRenderer}
     */
    public static AccessoryRenderer getOrDefaulted(Item item){
        return getRender(item).orElse(DefaultAccessoryRenderer.INSTANCE);
    }
}
