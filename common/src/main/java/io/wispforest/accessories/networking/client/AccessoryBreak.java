package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class AccessoryBreak extends AccessoriesPacket {

    private String slotName;
    private int entityId;
    private int slot;

    public AccessoryBreak(SlotReference slotReference) {
        super(false);

        this.slotName = slotReference.slotName();
        this.entityId = slotReference.entity().getId();
        this.slot = slotReference.slot();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.slotName);
        buf.writeInt(this.entityId);
        buf.writeVarInt(this.slot);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.slotName = buf.readUtf();
        this.entityId = buf.readInt();
        this.slot = buf.readVarInt();
    }

    @Override
    public void handle(Player player) {
        super.handle(player);

        var entity = player.level().getEntity(this.entityId);

        if(!(entity instanceof LivingEntity livingEntity)) {
            throw new IllegalStateException("Unable to handle a Break call due to the passed entity id not corresponding to a LivingEntity!");
        }

        var slotReference = SlotReference.of(livingEntity, this.slotName, this.slot);

        var capability = livingEntity.accessoriesCapability();

        var container = capability.getContainer(slotReference.type());

        var stack = container.getAccessories().getItem(slotReference.slot());

        var accessory = AccessoriesAPI.getAccessory(stack);

        if(accessory != null) accessory.onBreak(stack, slotReference);
    }
}
