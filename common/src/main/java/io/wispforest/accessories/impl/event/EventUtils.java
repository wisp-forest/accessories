package io.wispforest.accessories.impl.event;

import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class EventUtils {
    public static Event.Result toBusResult(TriState value){
        return switch (value) {
            case DEFAULT -> Event.Result.DEFAULT;
            case TRUE -> Event.Result.ALLOW;
            case FALSE -> Event.Result.DENY;
        };
    }

    public static TriState toTriState(Event.Result value){
        return switch (value) {
            case DEFAULT -> TriState.DEFAULT;
            case ALLOW -> TriState.TRUE;
            case DENY -> TriState.FALSE;
        };
    }

    public static <T> net.fabricmc.fabric.api.event.Event<T> createEventWithBus(Class<? super T> type, Supplier<Optional<IEventBus>> busSupplier, BiFunction<Optional<IEventBus>, T[], T> invokerFactory){
        return EventFactory.createArrayBacked(type, invokers -> invokerFactory.apply(busSupplier.get(), invokers));
    }
}
