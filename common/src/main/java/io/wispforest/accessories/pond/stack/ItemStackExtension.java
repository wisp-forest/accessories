package io.wispforest.accessories.pond.stack;

import io.wispforest.accessories.utils.ItemStackResize;
import io.wispforest.owo.util.EventStream;

public interface ItemStackExtension {
    EventStream<ItemStackResize> accessories$getResizeEvent();
}
