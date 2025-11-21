package io.wispforest.accessories.api.action;

import io.wispforest.accessories.Accessories;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Consumer;

public interface ActionResponse extends Reasonable {

    ActionResponse SUCCESS = of(true, Component.empty());

    boolean canPerformAction();

    @Override
    void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type);

    static ActionResponse of(boolean canPerformAction, Component reason) {
        return new ActionResponseBase(canPerformAction) {
            @Override
            public void gatherReason(Consumer<Component> messageAdditionCallback, Item.TooltipContext ctx, TooltipFlag type) {
                messageAdditionCallback.accept(reason);
            }
        };
    }

    static ActionResponse combineToSingleResponse(SequencedCollection<ActionResponse> responses) {
        return new CompoundResponse(responses.stream().toList());
    }

    @Nullable
    static Component getResponseReason(ActionResponse response, Item.TooltipContext ctx, TooltipFlag type) {
        var componentHolder = new MutableObject<Component>();

        response.gatherReason(componentHolder::setValue, ctx, type);

        var component = componentHolder.getValue();

        if (Accessories.DEBUG) {
            Objects.requireNonNull(component, "AccessResponse requires non null reason Component!");

            if (response != SUCCESS && component.getContents() == PlainTextContents.EMPTY) {
                throw new IllegalStateException("Custom AccessResponse requires non empty reason Component!");
            }
        }

        return component;
    }

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
