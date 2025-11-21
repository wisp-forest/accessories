package io.wispforest.accessories.api.action;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class CompoundResponse extends ActionResponseBase {
    private final List<ActionResponse> responses;

    public CompoundResponse(List<ActionResponse> responses) {
        super(canPerformAction(responses));

        this.responses = responses;
    }

    public static boolean canPerformAction(List<ActionResponse> responses) {
        for (var response : responses) {
            if (response.canPerformAction()) continue;

            return false;
        }

        return true;
    }

    public List<ActionResponse> getFilteredResponses(boolean filterSuccessActions) {
        return responses.stream().filter(response -> response.canPerformAction() != filterSuccessActions).toList();
    }

    @Override
    public void gatherReason(Consumer<Component> messageAddCallback, Item.TooltipContext ctx, TooltipFlag type) {
        for (var response : this.responses) {
            var baseMessage = Component.literal("- ");

            response.gatherReason(component -> messageAddCallback.accept(baseMessage.append(component)), ctx, type);
        }
    }

    public List<ActionResponse> responses() {
        return Collections.unmodifiableList(responses);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CompoundResponse) obj;
        return Objects.equals(this.responses, that.responses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responses);
    }

    @Override
    public String toString() {
        return "CompoundResponse[" +
            "responses=" + responses + ", " +
            "canPerformAction=" + canPerformAction + ']';
    }
}
