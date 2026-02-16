package io.wispforest.accessories.impl.core;

import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.action.ValidationState;
import io.wispforest.accessories.api.tooltip.ListTooltipAdder;
import io.wispforest.accessories.api.tooltip.impl.ListTooltipEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

public record UnknownResponse(ValidationState canPerformAction) implements ActionResponse {
    @Override
    public void addInfo(ListTooltipAdder adder, Item.TooltipContext ctx, TooltipFlag type) {
        adder.add(Component.literal(canPerformAction.isValid() ? "No Restrictions found!" : "Unknown restriction disallows such!"));
    }
}
