package io.wispforest.accessories.api.tooltip;

import net.minecraft.network.chat.FormattedText;

import java.util.stream.Stream;

///
/// Base Interface for handling the building of text data into [FormattedText] [Stream]. Typically,
/// this is for when a tooltip data needs to wrapped or prefixed with some indentation for better
/// format or prevent to long of tooltips
///
public interface FormattedTextBuilder {

    default Stream<FormattedText> build(TextWrapper wrapper) {
        return build(TextPrefixer.NONE, wrapper);
    }

    ///
    /// Using the prefixer and wrapper, build the given data into [FormattedText] typically
    /// from a [TooltipAdder] instance.
    ///
    Stream<FormattedText> build(TextPrefixer prefixer, TextWrapper wrapper);
}
