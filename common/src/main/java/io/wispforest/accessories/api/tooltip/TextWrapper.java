package io.wispforest.accessories.api.tooltip;

import io.wispforest.accessories.AccessoriesClientInternals;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.stream.Stream;

///
/// Helper object to wrap [FormattedText] to prevent it from being too long.
///
public abstract class TextWrapper {
    public static final TextWrapper NONE = new NoWrapping();

    public static TextWrapper createWrapper(int maxWidth) {
        return createWrapper(maxWidth, Style.EMPTY);
    }

    public static TextWrapper createWrapper(int maxWidth, Style overrideStyle) {
        return AccessoriesClientInternals.getInstance().createWrapper(maxWidth, overrideStyle);
    }

    public abstract Stream<FormattedText> wrap(FormattedText text);

    public abstract int hashCode();

    public abstract boolean equals(Object obj);
}

class NoWrapping extends TextWrapper {
    @Override
    public Stream<FormattedText> wrap(FormattedText text) {
        return Stream.of(text);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NoWrapping;
    }
}
