package io.wispforest.accessories.api.action;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.tooltip.ComponentBuilder;
import io.wispforest.accessories.api.tooltip.ListTooltipAdder;
import io.wispforest.accessories.utils.CollectionUtils;
import io.wispforest.accessories.utils.ComponentOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.*;

public class TagValidationResponse<T> extends ActionResponseBase {

    public static final TagCheckOperation ANY_MATCH = new TagCheckOperation() {
        @Override
        public <T> boolean isValid(Holder<T> holder, SequencedCollection<TagKey<T>> tags) {
            return tags.stream().anyMatch(holder::is);
        }

        @Override
        public <T> ComponentBuilder tagFormatting(Holder<T> holder, TagKey<T> tag) {
            return ValidationState.ofOrIrrelevant(holder.is(tag)).asColorBuilder();
        }

        @Override
        public String name() {
            return "any";
        }
    };

    public static final TagCheckOperation ALL_MATCH = new TagCheckOperation() {
        @Override
        public <T> boolean isValid(Holder<T> holder, SequencedCollection<TagKey<T>> tags) {
            return tags.stream().allMatch(holder::is);
        }

        @Override
        public <T> ComponentBuilder tagFormatting(Holder<T> holder, TagKey<T> tag) {
            return ValidationState.of(holder.is(tag)).asColorBuilder();
        }

        @Override
        public String name() {
            return "all";
        }
    };

    private final List<TagKey<T>> tags;
    private final Holder<T> entry;
    private final Registry<T> registry;
    private final TagCheckOperation operation;

    private final SequencedMap<TagKey<T>, ComponentBuilder> tagFormatting;

    public TagValidationResponse(Holder<T> entry, SequencedCollection<TagKey<T>> tags, TagCheckOperation operation) {
        super(operation.isValidChecked(entry, tags) ? ValidationState.VALID : ValidationState.IRRELEVANT);

        if (entry.kind().equals(Holder.Kind.DIRECT)) {
            throw new IllegalStateException("Unable to handle Holder '" + entry + "' as it was found to be Directly made instead of being a Reference which is required!");
        }

        this.tags = List.copyOf(tags);
        this.entry = entry;
        this.registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(entry.unwrapKey().get().registry());
        this.operation = operation;

        this.tagFormatting = tags.stream()
            .map(tagKey -> Map.entry(tagKey, operation.tagFormatting(entry, tagKey)))
            .collect(CollectionUtils.linkedMapCollector());
    }

    public Registry<T> registry() {
        return registry;
    }

    public List<TagKey<T>> getTags() {
        return tags;
    }

    public Holder<T> getEntry() {
        return entry;
    }

    public TagCheckOperation operation() {
        return operation;
    }

    @Override
    public void addInfo(ListTooltipAdder adder, Item.TooltipContext ctx, TooltipFlag type) {
        MutableComponent baseMsg;

        var stateName = this.canPerformAction().formatedName(false);

        if (type.isAdvanced() || type.hasShiftDown()) {
            var tags = ComponentOps.fromEntriesDivided(
                this.tagFormatting.sequencedEntrySet(),
                "validator.tag",
                entry -> entry.getValue()
                    .withArgs(Component.translatable(AccessoriesInternals.INSTANCE.getTagTranslation(entry.getKey())))
            );

            // accessories.tooltip.validator.tag.advanced.{valid | invalid}.{any | all | [...custom_name]}
            baseMsg = Accessories.translationWithArgs("tooltip.validator.tag.advanced", stateName, this.operation().name())
                .withArgs(tags);
        } else {
            // accessories.tooltip.validator.tag.basic.{valid | invalid}
            baseMsg = Accessories.translation("tooltip.validator.tag.simple", stateName);
        }

        adder.add(baseMsg);
    }

    public interface TagCheckOperation {

        default <T> boolean isValidChecked(Holder<T> holder, SequencedCollection<TagKey<T>> tags) {
            return holder.kind().equals(Holder.Kind.REFERENCE) && isValid(holder, tags);
        }

        <T> boolean isValid(Holder<T> holder, SequencedCollection<TagKey<T>> tags);

        String name();

        <T> ComponentBuilder tagFormatting(Holder<T> holder, TagKey<T> tag);
    }
}
