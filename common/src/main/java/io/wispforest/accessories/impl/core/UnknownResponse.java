package io.wispforest.accessories.impl.core;

import io.wispforest.accessories.api.action.ActionResponse;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public record UnknownResponse(boolean canPerformAction) implements ActionResponse {
    @Override
    public void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type) {
        messageAdditionCallback.accept(Component.literal(canPerformAction ? "No Restrictions found!" : "Unknown restriction disallows such!"));
    }
}
