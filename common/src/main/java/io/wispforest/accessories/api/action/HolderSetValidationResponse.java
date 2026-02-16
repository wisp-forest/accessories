package io.wispforest.accessories.api.action;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.tooltip.ComponentBuilder;
import io.wispforest.accessories.api.tooltip.ListTooltipAdder;
import io.wispforest.accessories.utils.CollectionUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.SequencedCollection;
import java.util.SequencedMap;

public class HolderSetValidationResponse<T> extends ActionResponseBase {

    private final HolderSet<T> set;
    private final Holder<T> holder;
    private final Registry<T> registry;
    private final SequencedMap<Holder<T>, ComponentBuilder> tagFormatting;

    public HolderSetValidationResponse(Registry<T> registry, TagKey<T> tag, Holder<T> holder) {
        this(registry, registry.get(tag).map(holders -> (HolderSet<T>) holders).orElse(HolderSet.empty()), holder);
    }

    public HolderSetValidationResponse(SequencedCollection<Holder<T>> holders, Holder<T> holder) {
        this(null, HolderSet.direct(holders.stream().toList()), holder);
    }

    public HolderSetValidationResponse(@Nullable Registry<T> registry, HolderSet<T> set, Holder<T> holder) {
        super(ValidationState.ofOrIrrelevant(set.stream().anyMatch(entry -> holder.is(holder))));

        if (holder.kind().equals(Holder.Kind.DIRECT)) {
            throw new IllegalStateException("Unable to handle Holder '" + holder + "' as it was found to be Directly made instead of being a Reference which is required!");
        }

        this.set = set;
        this.holder = holder;
        this.registry = registry == null ? (Registry<T>) BuiltInRegistries.REGISTRY.getValue(holder.unwrapKey().get().registry()) : registry;

        this.tagFormatting = set.stream()
            .map(entry -> Map.entry(entry, getFormatting(entry, entry)))
            .collect(CollectionUtils.linkedMapCollector());
    }

    private static <T> ComponentBuilder getFormatting(Holder<T> holder, Holder<T> entry) {
        return ValidationState.of(holder.is(entry)).asColorBuilder();
    }

    public Registry<T> registry() {
        return registry;
    }

    public SequencedMap<Holder<T>, ComponentBuilder> tagFormatting() {
        return tagFormatting;
    }

    public HolderSet<T> getSet() {
        return set;
    }

    public Holder<T> getEntry() {
        return holder;
    }

    @Override
    public void addInfo(ListTooltipAdder adder, Item.TooltipContext ctx, TooltipFlag type) {
        MutableComponent baseMsg;

        var infoType = this.canPerformAction().formatedName(false);

        var registryLangKey = registry.key().location().toLanguageKey();

        if (type.isAdvanced() || type.hasShiftDown()) {
            var tags = ComponentUtils.formatList(
                this.tagFormatting().entrySet(),
                Accessories.translation("tooltip.validator.set.entry_separator"),
                entry -> entry.getValue().withArgs(Component.translatable(AccessoriesInternals.INSTANCE.geEntryTranslation(entry.getKey())))
            );

            // accessories.tooltip.validator.set.advanced.{valid | invalid}
            baseMsg = Accessories.translationWithArgs("tooltip.validator.set.advanced", infoType)
                .withArgs(Component.translatable(registryLangKey  + ".plural"), tags);
        } else {
            // accessories.tooltip.validator.set.basic.{valid | invalid}
            baseMsg = Accessories.translationWithArgs("tooltip.validator.set.simple", infoType)
                .withArgs(Component.translatable(registryLangKey  + ".singular"));
        }

        adder.add(baseMsg);
    }
}
