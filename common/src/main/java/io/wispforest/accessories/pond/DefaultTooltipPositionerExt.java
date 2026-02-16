package io.wispforest.accessories.pond;

import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import org.joml.Vector2i;

import java.util.function.UnaryOperator;

public interface DefaultTooltipPositionerExt {

    static DefaultTooltipPositioner copyWith(DefaultTooltipPositioner positioner, PositionAdjuster adjuster) {
        return ((DefaultTooltipPositionerExt) positioner).accessories$copyWith(adjuster);
    }

    DefaultTooltipPositioner accessories$copyWith(PositionAdjuster adjuster);

    void accessories$setOperator(PositionAdjuster adjuster);

    interface PositionAdjuster {
        void adjust(int screenWidth, Vector2i tooltipPos, int tooltipWidth);
    }
}
