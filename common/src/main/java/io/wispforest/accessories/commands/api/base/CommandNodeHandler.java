package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.Branch;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.ArrayList;
import java.util.List;

public interface CommandNodeHandler<S> {

    default void modifyNode(String key, CommandAddition<S> addition) {
        modifyNode(new Key(key), addition);
    }

    default void modifyNode(Key key, CommandAddition<S> addition) {
        modifyNode(key.asArgumentList(), addition);
    }

    default void modifyNode(String key, List<Argument<?>> args, CommandAddition<S> addition) {
        modifyNode(new Key(key), args, addition);
    }

    default void modifyNode(Key key, List<Argument<?>> args, CommandAddition<S> addition) {
        getOrCreateHolder(key, args).andWith(addition);
    }

    default void modifyNode(List<Argument<?>> args, CommandAddition<S> addition) {
        getOrCreateHolder(args).andWith(addition);
    }

    default ArgumentBuilderHolder<S> getOrCreateHolder(Key key, List<Argument<?>> args) {
        var list = new ArrayList<>(args);

        list.addAll(0, key.asArgumentList());

        return getOrCreateHolder(list);
    }

    ArgumentBuilderHolder<S> getOrCreateHolder(List<Argument<?>> args);

    default BranchedCommandNodeHandler<S> modifyUnder(Key key) {
        return new BranchedCommandNodeHandler<S>() {
            @Override
            public Key branchKey() {
                return (CommandNodeHandler.this instanceof BranchedCommandNodeHandler branchedBuilder)
                        ? branchedBuilder.branchKey().child(key)
                        : key;
            }

            @Override
            public ArgumentBuilderHolder<S> getOrCreateHolder(List<Argument<?>> args) {
                return CommandNodeHandler.this.getOrCreateHolder(branchKey().child(key), args);
            }
        };
    }

    interface BranchedCommandNodeHandler<S> extends CommandNodeHandler<S>, Branch { }
}
