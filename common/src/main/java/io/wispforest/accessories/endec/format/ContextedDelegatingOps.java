package io.wispforest.accessories.endec.format;

import com.mojang.serialization.DynamicOps;
import io.wispforest.endec.SerializationContext;
import net.minecraft.resources.DelegatingOps;

public class ContextedDelegatingOps<T> extends DelegatingOps<T> implements ContextHolder {

    private final SerializationContext capturedContext;

    protected ContextedDelegatingOps(SerializationContext capturedContext, DynamicOps<T> delegate) {
        super(delegate);

        this.capturedContext = capturedContext;
    }

    public static <T> ContextedDelegatingOps<T> withContext(SerializationContext context, DynamicOps<T> delegate) {
        return new ContextedDelegatingOps<>(context, delegate);
    }

    public static <T> ContextedDelegatingOps<T> withoutContext(DynamicOps<T> delegate) {
        return new ContextedDelegatingOps(SerializationContext.empty(), delegate);
    }

    @Override
    public SerializationContext capturedContext() {
        return this.capturedContext;
    }
}
