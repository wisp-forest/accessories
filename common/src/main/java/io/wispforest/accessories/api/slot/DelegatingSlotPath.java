package io.wispforest.accessories.api.slot;

import java.util.List;

///
/// A wrapper class used for [SlotReference] that indicates that the given [SlotPath] delegates
/// to another object.
///
public sealed interface DelegatingSlotPath extends SlotPath permits SlotReference {
    SlotPath slotPath();

    @Override
    default SlotPath unpack() {
        return this.slotPath();
    }

    @Override
    default String slotName() {
        return slotPath().slotName();
    }

    @Override
    default int index() {
        return slotPath().index();
    }

    @Override
    default List<Integer> innerIndices() {
        return slotPath().innerIndices();
    }

    @Override
    default boolean isNested() {
        return slotPath().isNested();
    }

    @Override
    default String createString() {
        return slotPath().createString();
    }
}
