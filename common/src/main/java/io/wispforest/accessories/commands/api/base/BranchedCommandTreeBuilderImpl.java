package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.List;

public record BranchedCommandTreeBuilderImpl<S>(Key baseKey, CommandTreeBuilder<S, ?> parentBuilder) implements CommandTreeBuilder.BranchedCommandTreeBuilder<S, BranchedCommandTreeBuilderImpl<S>> {

    @Override
    public BranchedCommandTreeBuilderImpl<S> createLeaves(Key key, List<Argument<?>> commandArgs, CommandAddition<S> commandAddition) {
        parentBuilder.createLeaves(branchKey().child(key), commandArgs, commandAddition);

        return getThis();
    }

    @Override
    public Key branchKey() {
        return baseKey;
    }

    @Override
    public BranchedCommandTreeBuilderImpl<S> getThis() {
        return this;
    }
}
