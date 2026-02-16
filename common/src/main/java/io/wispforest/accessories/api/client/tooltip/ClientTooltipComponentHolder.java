package io.wispforest.accessories.api.client.tooltip;

import io.wispforest.accessories.api.tooltip.TooltipComponentHolder;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.List;

public interface ClientTooltipComponentHolder extends TooltipComponentHolder {
    List<ClientTooltipComponent> components();
}
