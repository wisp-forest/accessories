package io.wispforest.accessories.api.tooltip.impl;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.tooltip.ListTooltipAdder;
import io.wispforest.accessories.api.tooltip.TextPrefixer;
import io.wispforest.accessories.api.tooltip.TextWrapper;
import io.wispforest.accessories.api.tooltip.TooltipAdder;
import io.wispforest.accessories.utils.CollectionUtils;
import io.wispforest.accessories.utils.ComponentOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ListTooltipEntry implements ListTooltipAdder, TooltipAdder, TextPrefixer {

    public static final Component LIST_ENTRY = Accessories.translation("tooltip.equipment_reasoning.list_entry");
    public static final Component INDENT_ENTRY = Accessories.translation("tooltip.equipment_reasoning.indent_entry");

    protected final List<TooltipAdder> entries = new ArrayList<>();

    protected final @Nullable TextPrefixer overridePrefixer;

    public final FormattedText entryStart;
    public final FormattedText entryIndent;

    protected ListTooltipEntry() {
        this(LIST_ENTRY, INDENT_ENTRY);
    }

    protected ListTooltipEntry(FormattedText entryStart, FormattedText entryIndent) {
        this(null, entryStart, entryIndent);
    }

    protected ListTooltipEntry(@Nullable TextPrefixer overridePrefixer, FormattedText entryStart, FormattedText entryIndent) {
        this.overridePrefixer = overridePrefixer;
        this.entryStart = entryStart;
        this.entryIndent = entryIndent;
    }

    public static ListTooltipEntry of() {
        return new ListTooltipEntry();
    }

    public static ListTooltipEntry flatMap() {
        return new ListTooltipEntry(TextPrefixer.NONE, LIST_ENTRY, INDENT_ENTRY);
    }

    ///
    /// Add a [Component] to the list as a separate entry
    ///
    public ListTooltipEntry add(Component component) {
        entries.add(new TooltipEntry(List.of(component)));

        return this;
    }

    ///
    /// Start a new entry in the list, the returned callback allows for additions to the entry group
    ///
    public TooltipAdder adder() {
        var adder = new TooltipEntry();

        entries.add(adder);

        return adder;
    }

    @Override
    public ListTooltipEntry createListedEntry(FormattedText entryStart, FormattedText entryIndent) {
        var adder = new ListTooltipEntry(entryStart, entryIndent);

        entries.add(adder);

        return adder;
    }

    public boolean isEmpty() {
        for (var entry : entries) {
            if (!(entry instanceof ListTooltipAdder adder)) return false;
            if (!adder.isEmpty()) return false;
        }

        return true;
    }

    public int unpackedSize() {
        return entries.stream()
            .mapToInt(adder -> adder instanceof ListTooltipAdder list ? list.unpackedSize() : 1)
            .sum();
    }

    private @Nullable Integer baseHash = null;

    @Override
    public int hashCode() {
        if (baseHash == null) baseHash = Objects.hash(ComponentOps.getHashCode(entryStart), ComponentOps.getHashCode(entryIndent));

        return Objects.hash(baseHash, this.entries.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ListTooltipEntry other)) return false;
        return this.hashCode() == other.hashCode();
    }

    //--

    @Override
    public FormattedText handle(EntryMode mode, int index, FormattedText text) {
        return FormattedText.composite(mode == TextPrefixer.EntryMode.START ? entryStart : entryIndent, text);
    }

    @Override
    public Stream<FormattedText> build(TextPrefixer prefixer, TextWrapper wrapper) {
        return entries.stream()
            .flatMap(adder -> {
                var texts = adder.build(overridePrefixer != null ? overridePrefixer : this, wrapper);

                return CollectionUtils.mapWithIndex(texts, (index, text) -> prefixer.handle(TextPrefixer.EntryMode.INDENT, index, text));
            });
    }
}
