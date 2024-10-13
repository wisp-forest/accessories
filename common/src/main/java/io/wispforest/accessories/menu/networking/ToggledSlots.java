package io.wispforest.accessories.menu.networking;

import io.wispforest.owo.util.pond.OwoSlotExtension;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Map;

public record ToggledSlots(Map<Integer, Boolean> changedSlotStates) {

    public static void initMenu(AbstractContainerMenu menu) {
        menu.addServerboundMessage(ToggledSlots.class, (message) -> {
            message.changedSlotStates().forEach((index, state) -> {
                if (index >= menu.slots.size() || index < 0) return;

                var slot = ((OwoSlotExtension) menu.getSlot(index));

                if(state != slot.owo$getDisabledOverride()) slot.owo$setDisabledOverride(state);
            });
        });
    }
}
