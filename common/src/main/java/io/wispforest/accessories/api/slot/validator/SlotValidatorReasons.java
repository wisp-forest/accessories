package io.wispforest.accessories.api.slot.validator;

import io.wispforest.accessories.api.action.ActionResponse;
import net.minecraft.network.chat.Component;

public class SlotValidatorReasons {
    public static final Component INVALID_ITEM = Component.literal("The given Item is not allow within the slot");

    static final ActionResponse ALWAYS_VALID = ActionResponse.of(false, Component.literal("Anything can fit within this slot!"));
    static final ActionResponse ALWAYS_INVALID = ActionResponse.of(false, Component.literal("The slot is unable to take any Accessory, forever!"));
}
