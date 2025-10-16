package io.wispforest.accessories.commands.api.core;

import com.mojang.brigadier.builder.ArgumentBuilder;

import java.util.Objects;

public interface CommandAddition<S> {
    ArgumentBuilder<S, ?> addToBuilder(ArgumentBuilder<S, ?> builder);

    default CommandAddition<S> andWith(CommandAddition<S> addition) {
        Objects.requireNonNull(addition);
        return (builder) -> addition.addToBuilder(addToBuilder(builder));
    }
}
