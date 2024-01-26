package io.wispforest.accessories.api;

import java.util.Set;

/**
 * A Group of {@link SlotType}'s based on name used to cosmetically
 * group them together for the UI
 */
public interface SlotGroup {

    String name();

    int order();

    Set<String> slots();
}
