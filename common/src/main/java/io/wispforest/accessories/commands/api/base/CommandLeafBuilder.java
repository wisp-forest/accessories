package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.Branch;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.List;

public interface CommandLeafBuilder<S, B extends CommandLeafBuilder<S, B, T>, T extends Branch & CommandLeafBuilder<S, ?, T>> {
    B leaves(List<Argument<?>> startingArgs, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition);

    default B branch(List<String> keyParts, LeafBuilder<S, T> builder) {
        return branch(new Key(keyParts), builder);
    }

    default B branch(String key, LeafBuilder<S, T> builder) {
        return branch(new Key(key), builder);
    }

    default B branch(Key baseKey, LeafBuilder<S, T> builder) {
        builder.addLeaves(branch(baseKey));

        return getThis();
    }

    default T branch(List<String> keyParts) {
        return branch(new Key(keyParts));
    }

    default T branch(String key) {
        return branch(new Key(key));
    }

    T branch(Key baseKey);

    interface LeafBuilder<S, T>  {
        void addLeaves(T builder);
    }

    B getThis();
}
