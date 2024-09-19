package io.wispforest.accessories.impl.event;

import net.fabricmc.fabric.api.event.Event;

import java.util.function.Function;

public class WrappedEvent<T, W> extends Event<T> {

    private final Event<W> targetEvent;
    private final Function<T, W> conversionFunc;

    @Deprecated
    public WrappedEvent(Event<W> targetEvent, Function<T, W> conversionFunc){
        this.targetEvent = targetEvent;
        this.conversionFunc = conversionFunc;
    }

    public WrappedEvent(Event<W> targetEvent, Function<T, W> conversionFunc, Function<Event<W>, T> invokerBuilder){
        this.targetEvent = targetEvent;
        this.conversionFunc = conversionFunc;

        this.invoker = invokerBuilder.apply(targetEvent);
    }

    @Override
    public void register(T listener) {
        var wrappedListener = this.conversionFunc.apply(listener);

        this.targetEvent.register(wrappedListener);
    }


}
