package io.wispforest.accessories.api.slot.validator;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.action.ActionResponse;
import net.minecraft.network.chat.Component;

public class SlotValidatorReasons {
    public static final Component INVALID_ITEM = Component.literal("The given Item is not allowed within the slot");

    static final ActionResponse ALWAYS_VALID = ActionResponse.of(false, Accessories.translation("tooltip.validator.always_valid"));
    static final ActionResponse ALWAYS_INVALID = ActionResponse.of(false, Accessories.translation("tooltip.validator.always_invalid"));
}
