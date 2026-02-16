package io.wispforest.accessories.api.tooltip.impl;

import io.wispforest.accessories.api.tooltip.TextPrefixer;
import io.wispforest.accessories.api.tooltip.TextWrapper;
import io.wispforest.accessories.api.tooltip.TooltipAdder;
import io.wispforest.accessories.utils.CollectionUtils;
import io.wispforest.accessories.utils.ComponentOps;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TooltipEntry implements TooltipAdder {

    protected TooltipEntry() {
        this(new ArrayList<>());
    }

    protected TooltipEntry(List<Component> entries) {
        this.entries = entries;
    }

    public static TooltipEntry of() {
        return new TooltipEntry();
    }

    private final List<Component> entries;

    @Override
    public TooltipAdder add(Component component) {
        entries.add(component);

        resetHash();

        return this;
    }

    private boolean shouldCreateHash = true;
    private int hashCode = 0;

    protected void resetHash() {
        shouldCreateHash = true;
    }

    @Override
    public int hashCode() {
        if (shouldCreateHash) {
            var hashes = new Object[entries.size()];
            for (int i = 0; i < entries.size(); i++) hashes[i] = ComponentOps.getHashCode(entries.get(i));
            hashCode = Objects.hash(hashes);
            shouldCreateHash = false;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TooltipEntry other)) return false;
        return this.hashCode() == other.hashCode();
    }

    public List<Component> entries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public Stream<FormattedText> build(TextPrefixer prefixer, TextWrapper wrapper) {
        return this.entries.stream().flatMap(entry -> {
            return CollectionUtils.mapWithIndex(wrapper.wrap(entry), (innerIndex, wrappedText) -> {
                var mode = innerIndex == 0 ? TextPrefixer.EntryMode.START : TextPrefixer.EntryMode.INDENT;

                return prefixer.handle(mode, innerIndex, wrappedText);
            });
        });
    }
}
