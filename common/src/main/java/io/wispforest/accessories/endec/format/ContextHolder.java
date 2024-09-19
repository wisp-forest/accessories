package io.wispforest.accessories.endec.format;

import io.wispforest.endec.SerializationContext;

public interface ContextHolder {

    SerializationContext capturedContext();
}
