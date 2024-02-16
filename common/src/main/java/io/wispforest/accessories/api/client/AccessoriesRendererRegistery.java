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

    public static void registerRenderer(Item item, AccessoryRenderer renderer){
        RENDERERS.put(item, renderer);
    }

    public static Optional<AccessoryRenderer> getRender(Item item){
        return Optional.ofNullable(RENDERERS.get(item));
    }

    public static AccessoryRenderer getOrDefaulted(Item item){
        return getRender(item).orElse(DefaultAccessoryRenderer.INSTANCE);
    }
}
