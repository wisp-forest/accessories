package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.gui.AccessoriesInternalSlot;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record SyncCosmeticToggle(String slotName, int slotIndex) implements AccessoriesPacket {

    public static final Endec<SyncCosmeticToggle> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("slotName", SyncCosmeticToggle::slotName),
            Endec.VAR_INT.fieldOf("slotIndex", SyncCosmeticToggle::slotIndex),
            SyncCosmeticToggle::new
    );

    public static SyncCosmeticToggle of(SlotType slotType, int slotIndex) {
        return new SyncCosmeticToggle(slotType.name(), slotIndex);
    }

    @Override
    public void handle(Player player) {
        if(player.level().isClientSide()) return;

        var capability = player.accessoriesCapability();

        if(capability == null) return;

        var slotType = SlotTypeLoader.getSlotType(player.level(), this.slotName);

        if(slotType == null) return;

        var container = capability.getContainer(slotType);

        var renderOptions = container.renderOptions();

        renderOptions.set(this.slotIndex, !container.shouldRender(this.slotIndex));

        container.markChanged(false);
    }
}
