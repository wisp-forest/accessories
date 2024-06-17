package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.networking.AccessoriesPacket;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record AccessoryBreak(int entityId, String slotName, int slotIndex) implements AccessoriesPacket {

    public static Endec<AccessoryBreak> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", AccessoryBreak::entityId),
            Endec.STRING.fieldOf("slotName", AccessoryBreak::slotName),
            Endec.VAR_INT.fieldOf("slotIndex", AccessoryBreak::slotIndex),
            AccessoryBreak::new
    );

    public static AccessoryBreak of(SlotReference slotReference) {
        return new AccessoryBreak(slotReference.entity().getId(), slotReference.slotName(), slotReference.slot());
    }

    @Override
    public void handle(Player player) {
        var entity = player.level().getEntity(this.entityId);

        if(!(entity instanceof LivingEntity livingEntity)) {
            throw new IllegalStateException("Unable to handle a Break call due to the passed entity id not corresponding to a LivingEntity!");
        }

        var slotReference = SlotReference.of(livingEntity, this.slotName, this.slotIndex);

        var capability = livingEntity.accessoriesCapability();

        var container = capability.getContainer(slotReference.type());

        var stack = container.getAccessories().getItem(slotReference.slot());

        var accessory = AccessoriesAPI.getAccessory(stack);

        if(accessory != null) accessory.onBreak(stack, slotReference);
    }
}
