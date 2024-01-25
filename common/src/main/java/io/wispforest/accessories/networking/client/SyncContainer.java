package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.impl.SlotTypeImpl;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SyncContainer extends AccessoriesPacket {

    public CompoundTag containerTag;
    public int entityId;

    public SyncContainer(){}

    public SyncContainer(CompoundTag containerTag, int entityId){
        this.containerTag = containerTag;
        this.entityId = entityId;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(this.containerTag);
        buf.writeVarInt(this.entityId);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.containerTag = buf.readNbt();
        this.entityId = buf.readVarInt();
    }

    @Override
    public void handle(Player player) {
        super.handle(player);

        var entity = player.level().getEntity(entityId);

        if(!(entity instanceof LivingEntity livingEntity)) return;

        AccessoriesAccess.getHolder(livingEntity).read(containerTag);
    }
}
