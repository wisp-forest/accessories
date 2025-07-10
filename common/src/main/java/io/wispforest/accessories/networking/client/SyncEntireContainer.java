package io.wispforest.accessories.networking.client;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SyncEntireContainer(int entityId, NbtMapCarrier containerMap) {

    public static final StructEndec<SyncEntireContainer> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", SyncEntireContainer::entityId),
            NbtMapCarrier.ENDEC.fieldOf("containerTag", SyncEntireContainer::containerMap),
            SyncEntireContainer::new
    );

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void syncToAllTrackingAndSelf(ServerPlayer player) {
        syncTo(player, packet -> AccessoriesNetworking.sendToTrackingAndSelf(player, packet));
    }

    public static void syncTo(LivingEntity entity, Consumer<Record> handleCreator) {
        var capability = AccessoriesCapability.get(entity);

        if (capability == null) return;

        var carrier = NbtMapCarrier.of();

        ((AccessoriesHolderImpl) capability.getHolder()).write(carrier, SerializationContext.attributes(RegistriesAttribute.of(entity.level().registryAccess())));

        handleCreator.accept(new SyncEntireContainer(capability.entity().getId(), carrier));
    }

    @Environment(EnvType.CLIENT)
    public static void handlePacket(SyncEntireContainer packet, Player player) {
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

        var holder = ((AccessoriesHolderImpl) capability.getHolder());

//        if(entity instanceof Player) {
//            LOGGER.info("[SyncEntireContainer] Container data has been received on the client!");
//            LOGGER.info("[SyncEntireContainer] {}", containerMap);
//        }

        holder.read(packet.containerMap(), SerializationContext.attributes(RegistriesAttribute.of(level.registryAccess())));
        holder.init(capability);
    }
}
