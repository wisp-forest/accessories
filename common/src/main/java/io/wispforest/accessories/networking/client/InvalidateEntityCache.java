package io.wispforest.accessories.networking.client;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public record InvalidateEntityCache(int entityId) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final StructEndec<InvalidateEntityCache> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", InvalidateEntityCache::entityId),
            InvalidateEntityCache::new
    );

    public static void handlePacket(InvalidateEntityCache packet, Player player) {
        var level = player.level();
        var entity = level.getEntity(packet.entityId());

        if(entity == null) {
            LOGGER.error("Unable to Sync Container Data for a given Entity as it is null on the Client! [EntityId: {}]", packet.entityId());

            return;
        }

        if(!(entity instanceof LivingEntity livingEntity)) return;

        var capability = AccessoriesCapability.get(livingEntity);

        if(capability == null) {
            LOGGER.error("Unable to Sync Container Data for a given Entity as its Capability is null on the Client! [EntityId: {}]", packet.entityId());

            return;
        }

        var cache = ((AccessoriesHolderImpl) capability.getHolder()).getLookupCache();

        if(cache != null) cache.clearCache();
    }
}
