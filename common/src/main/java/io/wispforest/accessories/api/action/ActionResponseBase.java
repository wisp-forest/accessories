package io.wispforest.accessories.api.action;

public abstract class ActionResponseBase implements ActionResponse {
    protected final boolean canPerformAction;

    protected ActionResponseBase(boolean canPerformAction) {
        this.canPerformAction = canPerformAction;
    }

    @Override
    public boolean canPerformAction() {
        return this.canPerformAction;
    }
}
