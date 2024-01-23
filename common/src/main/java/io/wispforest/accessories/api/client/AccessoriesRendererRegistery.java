package io.wispforest.accessories.api.client;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AccessoriesRendererRegistery {

    private static final Map<Item, AccessoriesRenderer> RENDERERS = new HashMap<>();

    public static void registerRenderer(Item item, AccessoriesRenderer renderer){
        RENDERERS.put(item, renderer);
    }

    public static Optional<AccessoriesRenderer> getRender(Item item){
        return Optional.ofNullable(RENDERERS.get(item));
    }
}
