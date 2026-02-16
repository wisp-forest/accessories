package io.wispforest.accessories.utils;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesClientInternals;
import io.wispforest.accessories.api.tooltip.TooltipComponentBuilder;
import io.wispforest.accessories.api.tooltip.impl.TooltipEntry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.world.item.TooltipFlag;

import java.util.*;
import java.util.function.Function;

public class ComponentOps {

    private static final List<Integer> bitmasks = new ArrayList<>();

    static {
        var inst = AccessoriesClientInternals.getInstance();

        bitmasks.addAll(
            List.of(
                inst.createBitFlag(true, false, false),
                inst.createBitFlag(true, true, false),
                inst.createBitFlag(true, true, true),
                inst.createBitFlag(false, true, true),
                inst.createBitFlag(false, false, true)
            )
        );
    }

    public static ExtraInfoFooter attemptToAddExtraInfoFooter(SequencedCollection<String> baseKeys, TooltipFlag baseFlag, Function<TooltipFlag, Boolean> comparisonFunc) {
        var keys = new LinkedHashSet<>(baseKeys);

        for (var bitmask : bitmasks) {
            var newFlag = baseFlag.withMask(bitmask);
            var hasChanged = comparisonFunc.apply(newFlag);

            if (hasChanged) {
                if (newFlag.hasShiftDown() && !baseFlag.hasShiftDown()) keys.add("key.keyboard.left.shift");
                if (newFlag.hasAltDown() && !baseFlag.hasAltDown()) keys.add("key.keyboard.left.alt");
                if (newFlag.hasControlDown() && !baseFlag.hasControlDown()) keys.add("key.keyboard.left.control");
            }

            if (keys.size() >= 3) break;
        }

        return new ExtraInfoFooter(keys.stream().collect(CollectionUtils.linkedMapValueCollector(Component::translatable)));
    }

    public static final class ExtraInfoFooter {
        private final SequencedMap<String, MutableComponent> keyToFormattedText;

        public ExtraInfoFooter(SequencedMap<String, MutableComponent> keyToFormattedText) {
            this.keyToFormattedText = keyToFormattedText;
        }

        public void addTo(TooltipComponentBuilder builder) {
            if (keyToFormattedText.isEmpty()) return;

            builder
                .add(ComponentOps.extraInfoFooter(keyToFormattedText.sequencedValues()));
        }

        public ExtraInfoFooter combine(ExtraInfoFooter other) {
            var map = new LinkedHashMap<>(this.keyToFormattedText);

            other.keyToFormattedText.forEach(map::putIfAbsent);

            return new ExtraInfoFooter(map);
        }

        public SequencedMap<String, MutableComponent> keyToFormattedText() {
            return Collections.unmodifiableSequencedMap(keyToFormattedText);
        }
    }

    public static Component extraInfoFooter(SequencedCollection<MutableComponent> keys) {
        if (keys.isEmpty()) return Component.empty();

        return Accessories.translationWithArgs("tooltip.extra_info")
            .withArgs(fromEntriesAnyList(keys, "extra_info.list", Function.identity(), (startingEntries, lastEntry) -> {
                return Accessories.translationWithArgs("tooltip.extra_info.2_or_more")
                    .withArgs(startingEntries, lastEntry);
            }));
    }

    public static FormattedCharSequence getVisualOrderText(FormattedText text) {
        return (text instanceof Component component)
            ? component.getVisualOrderText()
            : Language.getInstance().getVisualOrder(text);
    }


    public static int getHashCode(FormattedText text) {
        var hasher = new HashingCharSink();
        ComponentOps.getVisualOrderText(text).accept(hasher);
        return hasher.hash;
    }

    private static class HashingCharSink implements FormattedCharSink {
        private int hash = 0;

        @Override
        public boolean accept(int charIndex, Style style, int codePoint) {
            hash = Objects.hash(hash, charIndex + Character.charCount(codePoint), style);

            return true;
        }
    }

    public interface AnyTextBuilder {
        MutableComponent build(Component startingEntries, Component lastEntry);
    }

    public static boolean isEmpty(Component component) {
        return component.getContents() == PlainTextContents.EMPTY;
    }

    public static Component validateComponent(Component component, String type) {
        if (Accessories.DEBUG) {
            Objects.requireNonNull(component, type + " requires non null reason Component!");

            if (isEmpty(component)) throw new IllegalStateException(type + " requires non empty reason Component!");
        }

        return component;
    }

    public static <T> MutableComponent fromEntriesAnyList(SequencedCollection<T> entries, String entryKeyPart, Function<T, MutableComponent> func, AnyTextBuilder builder) {
        if (entries.isEmpty()) return Component.empty();
        if (entries.size() == 1) return func.apply(entries.getFirst());

        var list = (entries instanceof List<T> list1 ? list1 : new ArrayList<>(entries))
            .subList(0, entries.size() - 1);

        return builder.build(ComponentUtils.formatList(list, Accessories.translation("tooltip." + entryKeyPart + ".entry_separator"), func::apply), func.apply(entries.getLast()));
    }

    public static <T> MutableComponent fromEntriesDivided(SequencedCollection<? extends T> entries, String entryKeyPart, Function<T, MutableComponent> func) {
        if (entries.isEmpty()) return Component.empty();
        if (entries.size() == 1) return func.apply(entries.getFirst());

        return ComponentUtils.formatList(entries, Accessories.translation("tooltip." + entryKeyPart + ".entry_separator"), func::apply);
    }
}
