package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.endec.EdmUtils;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.endec.format.nbt.NbtEndec;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.format.edm.EdmEndec;
import io.wispforest.endec.format.edm.EdmMap;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Catch all packet for handling syncing of containers and accessories within the main container
 * and cosmetic variant with the ability for such to be sync separately
 */
public record SyncContainerData(int entityId, Map<String, EdmMap> updatedContainers, Map<String, ItemStack> dirtyStacks, Map<String, ItemStack> dirtyCosmeticStacks) implements AccessoriesPacket {

    public static Endec<SyncContainerData> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", SyncContainerData::entityId),
            EdmEndec.MAP.mapOf().fieldOf("updatedContainers", SyncContainerData::updatedContainers),
            CodecUtils.ofCodec(ItemStack.OPTIONAL_CODEC).mapOf().fieldOf("dirtyStacks", SyncContainerData::dirtyStacks),
            CodecUtils.ofCodec(ItemStack.OPTIONAL_CODEC).mapOf().fieldOf("dirtyCosmeticStacks", SyncContainerData::dirtyCosmeticStacks),
            SyncContainerData::new
    );

    public static SyncContainerData of(LivingEntity livingEntity, Collection<AccessoriesContainer> updatedContainers, Map<String, ItemStack> dirtyStacks, Map<String, ItemStack> dirtyCosmeticStacks){
        var updatedContainerTags = new HashMap<String, EdmMap>();

        for (AccessoriesContainer updatedContainer : updatedContainers) {
            var syncCarrier = EdmUtils.newMap();

            ((AccessoriesContainerImpl) updatedContainer).write(syncCarrier, SerializationContext.attributes(RegistriesAttribute.of(livingEntity.registryAccess())), true);

            updatedContainerTags.put(updatedContainer.getSlotName(), syncCarrier);
        }

        return new SyncContainerData(livingEntity.getId(), updatedContainerTags, dirtyStacks, dirtyCosmeticStacks);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        var level = player.level();

        var entity = level.getEntity(entityId);

        if(!(entity instanceof LivingEntity livingEntity)) return;

        var capability = AccessoriesCapability.get(livingEntity);

        if(capability == null) return;

        var containers = capability.getContainers();

        var aContainerHasResized = false;

        for (var entry : this.updatedContainers.entrySet()) {
            if(!containers.containsKey(entry.getKey())) continue;

            var container = containers.get(entry.getKey());

            ((AccessoriesContainerImpl) container).read(entry.getValue(), SerializationContext.attributes(RegistriesAttribute.of(player.level().registryAccess())), true);

            if(container.getAccessories().wasNewlyConstructed()) aContainerHasResized = true;
        }

        for (var entry : dirtyStacks.entrySet()) {
            var parts = entry.getKey().split("/");

            var slot = parts[0];

            if(!containers.containsKey(slot)) continue;

            var container = containers.get(slot);

            try {
                container.getAccessories().setItem(Integer.parseInt(parts[1]), entry.getValue());
            } catch (NumberFormatException ignored){}
        }

        for (var entry : dirtyCosmeticStacks.entrySet()) {
            var parts = entry.getKey().split("/");

            var slot = parts[0];

            if(!containers.containsKey(slot)) continue;

            var container = containers.get(slot);

            try {
                container.getCosmeticAccessories().setItem(Integer.parseInt(parts[1]), entry.getValue());
            } catch (NumberFormatException ignored){}
        }

        if(player.containerMenu instanceof AccessoriesMenu menu && aContainerHasResized) {
            menu.reopenMenu();
            //AccessoriesClient.attemptToOpenScreen();
        }
    }
}
