package io.wispforest.accessories.api.action;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public interface Reasonable {
    void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type);
}
