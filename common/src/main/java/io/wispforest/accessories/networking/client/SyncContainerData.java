package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.impl.AccessoriesContainerImpl;
import io.wispforest.accessories.networking.CacheableAccessoriesPacket;
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
import java.util.Set;

/**
 * Catch all packet for handling syncing of containers and accessories within the main container
 * and cosmetic variant with the ability for such to be sync separately
 */
public class SyncContainerData extends CacheableAccessoriesPacket {

    private int entityId;
    private Map<String, CompoundTag> updatedContainers;
    private Map<String, ItemStack> dirtyStacks;
    private Map<String, ItemStack> dirtyCosmeticStacks;

    public SyncContainerData(){ super(); }

    public SyncContainerData(FriendlyByteBuf buf){
        super(buf);
    }

    public SyncContainerData(int entityId, Collection<AccessoriesContainer> updatedContainers, Map<String, ItemStack> dirtyStacks, Map<String, ItemStack> dirtyCosmeticStacks){
        super(false);

        this.entityId = entityId;

        var updatedContainerTags = new HashMap<String, CompoundTag>();

        for (AccessoriesContainer updatedContainer : updatedContainers) {
            var syncTag = new CompoundTag();

            ((AccessoriesContainerImpl) updatedContainer).write(syncTag, true);

            updatedContainerTags.put(updatedContainer.getSlotName(), syncTag);
        }

        this.updatedContainers = updatedContainerTags;

        this.dirtyStacks = dirtyStacks;
        this.dirtyCosmeticStacks = dirtyCosmeticStacks;
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();

        this.updatedContainers = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readNbt);

        this.dirtyStacks = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readItem);
        this.dirtyCosmeticStacks = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readItem);
    }

    @Override
    protected void writeUncached(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);

        buf.writeMap(this.updatedContainers, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeNbt);

        buf.writeMap(this.dirtyStacks, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeItem);
        buf.writeMap(this.dirtyCosmeticStacks, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeItem);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        super.handle(player);

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

            ((AccessoriesContainerImpl) container).read(entry.getValue(), true);

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
