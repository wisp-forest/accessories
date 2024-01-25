package io.wispforest.accessories.api;

import java.util.Set;

public interface SlotGroup {

    String name();

    int order();

    Set<String> slots();
}
