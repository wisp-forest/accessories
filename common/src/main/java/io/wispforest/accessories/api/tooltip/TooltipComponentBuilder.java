package io.wispforest.accessories.api.tooltip;

import io.wispforest.accessories.AccessoriesClientInternals;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.stream.Stream;

///
/// Low level way of building tooltips into [ClientTooltipComponent][net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent]'s
///
@ApiStatus.NonExtendable
public interface TooltipComponentBuilder extends TooltipComponentHolder {

    static TooltipComponentBuilder of() {
        return AccessoriesClientInternals.getInstance().createTooltipBuilder();
    }

    TooltipComponentBuilder divider(int height);

    TooltipComponentBuilder divider();

    TooltipComponentBuilder add(TooltipComponentBuilder builder);

    TooltipComponentBuilder add(FormattedText text);

    TooltipComponentBuilder addAll(Collection<? extends FormattedText> text);

    TooltipComponentBuilder addAll(TextPrefixer prefixer, FormattedTextBuilder builder);

    TooltipComponentBuilder add(TooltipComponentHolder holder);

    boolean isEmpty();
}
