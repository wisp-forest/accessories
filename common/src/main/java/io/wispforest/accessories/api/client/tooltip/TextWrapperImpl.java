package io.wispforest.accessories.api.client.tooltip;

import io.wispforest.accessories.api.tooltip.TextWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.Objects;
import java.util.stream.Stream;

public final class TextWrapperImpl extends TextWrapper {
    private final int maxWidth;
    private final Style overrideStyle;

    public TextWrapperImpl(int maxWidth, Style overrideStyle) {
        this.maxWidth = maxWidth;
        this.overrideStyle = overrideStyle;
    }

    public int maxWidth() {
        return maxWidth;
    }

    public Style overrideStyle() {
        return overrideStyle;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof TextWrapperImpl other)) return false;
        return this.maxWidth == other.maxWidth &&
            Objects.equals(this.overrideStyle, other.overrideStyle);
    }

    @Override
    public Stream<FormattedText> wrap(FormattedText text) {
        return Minecraft.getInstance().font.getSplitter().splitLines(text, maxWidth, overrideStyle).stream();
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxWidth, overrideStyle);
    }

    @Override
    public String toString() {
        return "TextWrapperImpl[" +
            "maxWidth=" + maxWidth + ", " +
            "overrideStyle=" + overrideStyle + ']';}

}
