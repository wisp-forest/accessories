package io.wispforest.accessories.api.tooltip;

import net.minecraft.network.chat.Component;

///
/// Helper class used to collect [Component]'s for tooltip info
///
public interface TooltipAdder extends FormattedTextBuilder {

    ///
    /// Add the given component as a new line entry for the adder
    ///
    TooltipAdder add(Component component);
}

