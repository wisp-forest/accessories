package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.gui.AccessoriesInternalSlot;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class SyncCosmeticToggle extends AccessoriesPacket {

    private String slotName;
    private int slotIndex;

    public SyncCosmeticToggle() {}

    public SyncCosmeticToggle(SlotType slotType, int slotIndex) {
        super(false);

        this.slotName = slotType.name();
        this.slotIndex = slotIndex;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.slotName);
        buf.writeInt(this.slotIndex);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.slotName = buf.readUtf();
        this.slotIndex = buf.readInt();
    }

    @Override
    public void handle(Player player) {
        super.handle(player);

        if(player.level().isClientSide()) return;

        var capability = player.accessoriesCapability();

        if(capability == null) return;

        var slotType = SlotTypeLoader.getSlotType(player.level(), this.slotName);

        if(slotType == null) return;

        var container = capability.getContainer(slotType);

        var renderOptions = container.renderOptions();

        renderOptions.set(this.slotIndex, !container.shouldRender(this.slotIndex));

        container.markChanged();
    }
}
