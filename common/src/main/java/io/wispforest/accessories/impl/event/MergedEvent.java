package io.wispforest.accessories.impl.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.neoforged.bus.api.IEventBus;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Wrapped Version of {@link net.fabricmc.fabric.impl.base.event.ArrayBackedEvent} with the ability to call out to Forge Like Platforms with
 * a {@link IEventBus} after Event Calls
 */
public class MergedEvent<T> extends Event<T> {

    public final Event<T> fabricEventCall;

    public MergedEvent(Class<? super T> type, Supplier<Optional<IEventBus>> busSupplier, BiFunction<Optional<IEventBus>, T[], T> invokerFactory){
        this.fabricEventCall = EventFactory.createArrayBacked(type, invokers -> {
            return invokerFactory.apply(busSupplier.get(), invokers);
        });
    }

    @Override
    public void register(T listener) {
        fabricEventCall.register(listener);
    }
}
