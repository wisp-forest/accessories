package io.wispforest.accessories.networking.client;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.menu.variants.AccessoriesMenuBase;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Stream;

/**
 * Catch all packet for handling syncing of containers and accessories within the main container
 * and cosmetic variant with the ability for it to be sync separately
 */
public record SyncContainerData(int entityId, Map<String, NbtMapCarrier> updatedContainers, Map<String, ItemStack> dirtyStacks, Map<String, ItemStack> dirtyCosmeticStacks) {

    public static StructEndec<SyncContainerData> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", SyncContainerData::entityId),
            NbtMapCarrier.ENDEC.mapOf().fieldOf("updatedContainers", SyncContainerData::updatedContainers),
            CodecUtils.toEndec(ItemStack.OPTIONAL_CODEC).mapOf().fieldOf("dirtyStacks", SyncContainerData::dirtyStacks),
            CodecUtils.toEndec(ItemStack.OPTIONAL_CODEC).mapOf().fieldOf("dirtyCosmeticStacks", SyncContainerData::dirtyCosmeticStacks),
            SyncContainerData::new
    );

    public static SyncContainerData of(LivingEntity livingEntity, Collection<AccessoriesContainer> updatedContainers, Map<String, ItemStack> dirtyStacks, Map<String, ItemStack> dirtyCosmeticStacks){
        var updatedContainerTags = new HashMap<String, NbtMapCarrier>();

        for (AccessoriesContainer updatedContainer : updatedContainers) {
            var syncCarrier = NbtMapCarrier.of();

            ((AccessoriesContainerImpl) updatedContainer).write(syncCarrier, SerializationContext.attributes(RegistriesAttribute.of(livingEntity.registryAccess())), true);

            updatedContainerTags.put(updatedContainer.getSlotName(), syncCarrier);
        }

        return new SyncContainerData(livingEntity.getId(), updatedContainerTags, dirtyStacks, dirtyCosmeticStacks);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    @Environment(EnvType.CLIENT)
    public static void handlePacket(SyncContainerData packet, Player player) {
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

        var containers = capability.getContainers();

        var aContainerHasResized = false;

        Set<String> changedContainers = new HashSet<>();

        //--

        Set<String> invalidSyncedContainers = new HashSet<>();

        for (var entry : packet.updatedContainers().entrySet()) {
            if (!containers.containsKey(entry.getKey())) {
                invalidSyncedContainers.add(entry.getKey());

                continue;
            }

            var container = containers.get(entry.getKey());

            changedContainers.add(container.getSlotName());

            ((AccessoriesContainerImpl) container).read(entry.getValue(), SerializationContext.attributes(RegistriesAttribute.of(player.level().registryAccess())), true);

            if (container.getAccessories().wasNewlyConstructed()) aContainerHasResized = true;
        }

        if(!invalidSyncedContainers.isEmpty()) {
            LOGGER.warn("Unable to sync container data for the following containers: {}", invalidSyncedContainers);
        }

        //--

        Set<String> invalidDirtyStackContainers = new HashSet<>();

        for (var entry : packet.dirtyStacks().entrySet()) {
            var parts = entry.getKey().split("/");

            var slot = parts[0];

            if(!containers.containsKey(slot)) {
                invalidDirtyStackContainers.add(slot);

                continue;
            }

            var container = containers.get(slot);

            changedContainers.add(container.getSlotName());

            try {
                container.getAccessories().setItem(Integer.parseInt(parts[1]), entry.getValue());
            } catch (NumberFormatException ignored){}
        }

        if(!invalidDirtyStackContainers.isEmpty()) {
            LOGGER.warn("Unable to sync dirty stack data for the following containers: {}", invalidSyncedContainers);
        }

        //--

        Set<String> invalidDirtyCosmeticContainers = new HashSet<>();

        for (var entry : packet.dirtyCosmeticStacks().entrySet()) {
            var parts = entry.getKey().split("/");

            var slot = parts[0];

            if(!containers.containsKey(slot)) {
                invalidDirtyCosmeticContainers.add(slot);

                continue;
            }

            var container = containers.get(slot);

            changedContainers.add(container.getSlotName());

            try {
                container.getCosmeticAccessories().setItem(Integer.parseInt(parts[1]), entry.getValue());
            } catch (NumberFormatException ignored){}
        }

        if(!invalidDirtyCosmeticContainers.isEmpty()) {
            LOGGER.warn("Unable to sync dirty stack data for the following containers: {}", invalidSyncedContainers);
        }

        //--

        var cache = ((AccessoriesHolderImpl) capability.getHolder()).getLookupCache();

        if (cache != null) {
            changedContainers.forEach(cache::clearContainerCache);
        }

        if(player.containerMenu instanceof AccessoriesMenuBase menu && aContainerHasResized) {
            menu.reopenMenu();
            //AccessoriesClient.attemptToOpenScreen();
        }
    }
}
