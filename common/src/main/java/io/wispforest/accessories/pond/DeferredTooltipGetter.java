package io.wispforest.accessories.pond;

import io.wispforest.accessories.api.client.tooltip.DeferredTooltip;
import org.jetbrains.annotations.Nullable;

public interface DeferredTooltipGetter {
    @Nullable DeferredTooltip accessories$getTooltip();
}
