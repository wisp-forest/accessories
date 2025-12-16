package io.wispforest.accessories.api.client;

import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

import java.util.function.Supplier;

public class DefaultedContextKey<T> extends ContextKey<T> {
    private final Supplier<T> defaultValue;

    public DefaultedContextKey(Identifier name, Supplier<T> defaultValue) {
        super(name);

        this.defaultValue = defaultValue;
    }

    public T getDefaultValue() {
        return this.defaultValue.get();
    }
}
