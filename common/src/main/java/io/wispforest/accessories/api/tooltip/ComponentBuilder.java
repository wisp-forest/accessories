package io.wispforest.accessories.api.tooltip;

import net.minecraft.network.chat.MutableComponent;

///
/// Helper interface to used to build a [MutableComponent] from passed in args. Useful for
/// static method returns for namespaced translation components.
///
public interface ComponentBuilder {
    MutableComponent withArgs(Object... args);
}
