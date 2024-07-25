package io.wispforest.accessories.networking.client;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public record SyncEntireContainer(int entityId, NbtMapCarrier containerMap) implements HandledPacketPayload {

    public static final Endec<SyncEntireContainer> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", SyncEntireContainer::entityId),
            NbtMapCarrier.ENDEC.fieldOf("containerTag", SyncEntireContainer::containerMap),
            SyncEntireContainer::new
    );

    private static final Logger LOGGER = LogUtils.getLogger();

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        var entity = player.level().getEntity(entityId);

        if(entity == null) {
            LOGGER.info("Unable to Sync Container Data for a given Entity as it is null on the Client! [EntityId: {}]", entityId);

            return;
        }

        if(!(entity instanceof LivingEntity livingEntity)) return;

        var capability = AccessoriesCapability.get(livingEntity);

        if(capability == null) return;

        ((AccessoriesHolderImpl) capability.getHolder()).read(containerMap, SerializationContext.empty());

        ((AccessoriesHolderImpl) capability.getHolder()).init(capability);
    }
}
