package io.wispforest.accessories.commands.api.base;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArgumentBuilderHolder<S> {
    private final CommandNodeHandler<S> handler;
    private final Key key;

    private final ArgumentBuilder<S, ?> baseNode;
    private CommandAddition<S> commandAddition = builder -> builder;

    ArgumentBuilderHolder(CommandNodeHandler<S> handler, Key key, ArgumentBuilder<S, ?> baseNode) {
        Objects.requireNonNull(baseNode, "NodeTreeHelper was attempted to be constructed with a null base node");

        this.handler = handler;
        this.key = key;
        this.baseNode = baseNode;
    }

    ArgumentBuilder<S, ?> addToNode() {
        return commandAddition.addToBuilder(baseNode);
    }

    public Key key() {
        return key;
    }

    public ArgumentBuilderHolder<S> andWith(CommandAddition<S> func) {
        commandAddition = commandAddition.andWith(func);

        return this;
    }

    public ArgumentBuilderHolder<S> getOrCreateChild(Key key, List<Argument<?>> args) {
        var list = new ArrayList<>(args);

        list.addAll(0, key.asArgumentList());

        return getOrCreateChild(list);
    }

    public ArgumentBuilderHolder<S> getOrCreateChild(List<Argument<?>> args) {
        return handler.getOrCreateHolder(key, args);
    }
}
