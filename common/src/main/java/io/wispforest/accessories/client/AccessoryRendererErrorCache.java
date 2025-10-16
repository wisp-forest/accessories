package io.wispforest.accessories.client;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.UUID;

public class AccessoryRendererErrorCache {
    private static final ClientDelayedCache<Key> ERROR_CACHE = new ClientDelayedCache<>(4000);

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void logIfTimeAllotted(UUID uuid, ItemStack stack, AccessoryRenderer renderer, Throwable e) {
        var key = new Key(uuid, ItemStack.hashItemAndComponents(stack), AccessoriesRendererRegistry.getRendererId(renderer));

        // TODO: Maybe also send a toast???
        if (ERROR_CACHE.hasAllottedTime(key, 10)) {
            LOGGER.error("[AccessoryRendererError] Unable to use the given Renderer [{}] to render the given item [{}] for UUID [{}] due to the given error: ", key.rendererId(), stack, uuid, e);
        }
    }

    public record Key(UUID entityUUID, int itemStackHash, ResourceLocation rendererId) { }
}
