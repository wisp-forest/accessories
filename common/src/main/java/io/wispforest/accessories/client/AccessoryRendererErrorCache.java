package io.wispforest.accessories.client;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.UUID;

public class AccessoryRendererErrorCache {
    private static final ClientDelayedCache<Key> ERROR_CACHE = new ClientDelayedCache<>(4000);

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void logIfTimeAllotted(Entity entity, ItemStack stack, AccessoryRenderer renderer, Throwable e) {
        var key = new Key(entity.getUUID(), ItemStack.hashItemAndComponents(stack), AccessoriesRendererRegistry.getRendererId(renderer));

        // TODO: Maybe also send a toast???
        if (ERROR_CACHE.hasAllottedTime(key, 10)) {
            LOGGER.error("[AccessoryRendererError] Unable to use the given Renderer [{}] to render the given item [{}] for [{}] due to the given error: ", key.rendererId(), stack, entity, e);
        }
    }

    public record Key(UUID entityUUID, int itemStackHash, ResourceLocation rendererId) { }
}
