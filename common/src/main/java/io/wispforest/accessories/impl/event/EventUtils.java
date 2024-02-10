package io.wispforest.accessories.impl.event;

import net.fabricmc.fabric.api.util.TriState;
import net.neoforged.bus.api.Event;

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
}
