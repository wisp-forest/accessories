package io.wispforest.accessories.api.events;

import org.jetbrains.annotations.Nullable;

public abstract class ReturnableEvent<T> extends BaseEvent {

    @Nullable
    private T state = null;

    public void setReturn(T state) {
        this.state = state;
    }

    public T getReturn() {
        return this.state;
    }

    public T getReturnOrDefault(T defaultValue) {
        var returnValue = this.getReturn();

        return returnValue != null ? returnValue : defaultValue;
    }

    public boolean hasReturn() {
        return this.state != null;
    }
}
