package io.wispforest.accessories.api.action;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.SequencedCollection;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public abstract class TagValidationResponse<T> extends ActionResponseBase {

    public static final TagCheckOperation ANY_MATCH = TagCheckOperation.of((holder, tags) -> tags.stream().anyMatch(holder::is));
    public static final TagCheckOperation ALL_MATCH = TagCheckOperation.of((holder, tags) -> tags.stream().allMatch(holder::is));

    private final SequencedCollection<TagKey<T>> tags;
    private final Holder<T> entry;

    protected TagValidationResponse(Holder<T> entry, SequencedCollection<TagKey<T>> tags) {
        this(entry, tags, ANY_MATCH);
    }

    protected TagValidationResponse(Holder<T> entry, SequencedCollection<TagKey<T>> tags, TagCheckOperation operation) {
        super(operation.isValidChecked(entry, tags));

        this.tags = tags;
        this.entry = entry;
    }

    public SequencedCollection<TagKey<T>> getTags() {
        return tags;
    }

    public Holder<T> getEntry() {
        return entry;
    }

    @Override
    public abstract void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type);

    public interface TagCheckOperation {
        <T> boolean isValid(Holder<T> holder, SequencedCollection<TagKey<T>> tags);

        default <T> boolean isValidChecked(Holder<T> holder, SequencedCollection<TagKey<T>> tags) {
            if (holder.kind().equals(Holder.Kind.DIRECT)) {
                throw new IllegalStateException("Unable to handle Holder '" + holder + "' as it was found to be Directly made instead of being a Reference which is required!");
            }

            return isValid(holder, tags);
        }

        static <T> TagCheckOperation of(BiPredicate<Holder<T>, SequencedCollection<TagKey<T>>> predicate) {
            return new TagCheckOperation() {
                @Override
                public <A> boolean isValid(Holder<A> holder, SequencedCollection<TagKey<A>> tags) {
                    return ((BiPredicate) predicate).test(holder, tags);
                }
            };
        }
    }
}
