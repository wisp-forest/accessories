package io.wispforest.accessories.api.tooltip;

import io.wispforest.accessories.api.tooltip.impl.ListTooltipEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

///
/// Helper class used to collect Lists of [Component]'s for tooltip info mainly in formated entry lists
///
public interface ListTooltipAdder extends TooltipAdder {

    default ListTooltipAdder createListedEntry() {
        return createListedEntry(ListTooltipEntry.LIST_ENTRY, ListTooltipEntry.INDENT_ENTRY);
    }

    ///
    /// Create Nested list entry for list within a list data
    ///
    ListTooltipAdder createListedEntry(FormattedText entryStart, FormattedText entryIndent);

    ///
    /// Check if there is any tooltip data exists within the children of the adder
    ///
    boolean isEmpty();

    ///
    /// Get the size of the current adder tooltip data from within its children
    ///
    int unpackedSize();
}
