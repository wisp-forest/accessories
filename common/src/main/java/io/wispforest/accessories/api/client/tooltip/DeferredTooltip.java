package io.wispforest.accessories.api.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DeferredTooltip(
    Font font,
    List<ClientTooltipComponent> components,
    int x,
    int y,
    ClientTooltipPositioner positioner,
    @Nullable Identifier background,
    boolean focused,
    Runnable runnable) implements Runnable, ClientTooltipComponentHolder {

    @Override
    public void run() {
        this.runnable.run();
    }
}
