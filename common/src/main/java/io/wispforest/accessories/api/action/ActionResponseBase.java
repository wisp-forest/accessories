package io.wispforest.accessories.api.action;

///
/// Base version of [ActionResponse] that acts a starting point for
/// custom responses.
///
public abstract class ActionResponseBase implements ActionResponse {
    protected final ValidationState canPerformAction;

    protected ActionResponseBase(boolean canPerformAction) {
        this(ValidationState.of(canPerformAction));
    }

    protected ActionResponseBase(ValidationState canPerformAction) {
        this.canPerformAction = canPerformAction;
    }

    @Override
    public ValidationState canPerformAction() {
        return canPerformAction;
    }
}
