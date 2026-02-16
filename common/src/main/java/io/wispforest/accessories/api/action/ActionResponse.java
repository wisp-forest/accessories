package io.wispforest.accessories.api.action;

import io.wispforest.accessories.api.tooltip.ListTooltipAdder;
import io.wispforest.accessories.utils.ComponentOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

///
/// Acts as a holder for a [ValidationState] and message used to tell if
/// an action can or can not be performed with a default value  being letting
/// the caller decided the outcome.
///
public interface ActionResponse extends ReasonProvider {

    static ActionResponse SUCCESS = of(true, Component.empty());

    //--

    static ActionResponse of(boolean canPerformAction, Component reason) {
        return of(ValidationState.of(canPerformAction), reason);
    }

    static ActionResponse of(ValidationState state, Component reason) {
        return of(state, (callback, ctx, type) -> {
            callback.add(ComponentOps.validateComponent(reason, "ActionResponse"));
        });
    }

    static ActionResponse of(ValidationState state, ReasonProvider provider) {
        return new ActionResponseBase(state) {
            @Override
            public void addInfo(ListTooltipAdder adder, Item.TooltipContext ctx, TooltipFlag type) {
                provider.addInfo(adder, ctx, type);
            }
        };
    }

    //--

    ///
    /// @return The resultant of the response indicating either its [VALID][ValidationState#VALID],
    /// [INVALID][ValidationState#INVALID], or [IRRELEVANT][ValidationState#IRRELEVANT] to the
    /// outcome of the action check.
    ///
    ValidationState canPerformAction();

    @Override
    void addInfo(ListTooltipAdder adder, Item.TooltipContext ctx, TooltipFlag flag);

    //--

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

}
