package io.wispforest.accessories.api.tooltip;

import net.minecraft.network.chat.FormattedText;

///
/// Helper object used to append to the start of a given [FormattedText] either for
/// indentation and/or list entry marker text.
///
public interface TextPrefixer {
    TextPrefixer NONE = (mode, index, text) -> text;

    FormattedText handle(EntryMode mode, int index, FormattedText text);

    enum EntryMode {
        START,
        INDENT
    }
}
