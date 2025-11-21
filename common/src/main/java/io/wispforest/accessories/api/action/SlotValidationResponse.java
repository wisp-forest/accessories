package io.wispforest.accessories.api.action;

import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public abstract class SlotValidationResponse extends ActionResponseBase {
    private final SlotType slotType;

    protected SlotValidationResponse(SlotType slotType, boolean canPerformAction) {
        super(canPerformAction);

        this.slotType = slotType;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    @Override
    public abstract void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type);
}
