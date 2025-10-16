package io.wispforest.accessories.utils;

import io.wispforest.owo.util.EventStream;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class EnhancedEventStream<T> extends EventStream<T> implements ConcurrentBarrier {

    private final Set<Runnable> subscriptionUpdates = new HashSet<>();

    private final Runnable onEmptyCallback;

    private boolean isSinking = false;

    private EnhancedEventStream(Function<List<T>, T> sinkFactory, MutableObject<ConcurrentBarrier> barrierHolder, Runnable onEmptyCallback) {
        super(sinkFactory);

        this.onEmptyCallback = onEmptyCallback;
        barrierHolder.setValue(this);
    }

    public static <T> EnhancedEventStream<T> of(BiFunction<List<T>, ConcurrentBarrier, T> sinkFactory, Runnable onEmptyCallback) {
        var currentlySinkingFlag = new MutableObject<ConcurrentBarrier>(null);

        return new EnhancedEventStream<T>(ts -> sinkFactory.apply(ts, value -> currentlySinkingFlag.getValue().setSinking(value)), currentlySinkingFlag, onEmptyCallback);
    }

    @Override
    public T sink() {
        this.isSinking = true;

        return super.sink();
    }

    @Override
    protected void addSubscriber(T subscriber) {
        if (this.isSinking) {
            subscriptionUpdates.add(() -> addSubscriber(subscriber));

            return;
        }

        super.addSubscriber(subscriber);
    }

    @Override
    protected void removeSubscriber(T subscriber) {
        if (this.isSinking) {
            subscriptionUpdates.add(() -> removeSubscriber(subscriber));

            return;
        }

        super.removeSubscriber(subscriber);

        if (this.subscribers.isEmpty()) {
            this.onEmptyCallback.run();
        }
    }

    @Override
    public void setSinking(boolean value) {
        this.isSinking = value;

        if (!value) {
            for (var subscriptionUpdate : this.subscriptionUpdates) {
                subscriptionUpdate.run();
            }

            this.subscriptionUpdates.clear();
        }
    }

}
