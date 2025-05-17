package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;

import java.util.*;

public interface CommandNodeHandler<S> {

    default void modifyNode(String key, CommandAddition<S> addition) {
        modifyNode(new Key(key), addition);
    }

    default void modifyNode(Key key, CommandAddition<S> addition) {
        modifyNode(key.path().stream().map(Argument::asKeyPath).toList(), addition);
    }

    default void modifyNode(String key, List<? extends Argument<?>> args, CommandAddition<S> addition) {
        modifyNode(new Key(key), args, addition);
    }

    default void modifyNode(List<? extends Argument<?>> args, CommandAddition<S> addition) {
        modifyNode(new Key(), args, addition);
    }

    void modifyNode(Key key, List<? extends Argument<?>> args, CommandAddition<S> addition);

    default BranchedCommandNodeHandler<S> modifyUnder(Key key) {
        return new BranchedCommandNodeHandler<S>() {
            @Override
            public Key branchKey() {
                return (CommandNodeHandler.this instanceof BranchedCommandNodeHandler branchedBuilder)
                        ? branchedBuilder.branchKey().child(key)
                        : key;
            }

            @Override
            public void modifyNode(Key key, List<? extends Argument<?>> args, CommandAddition<S> addition) {
                CommandNodeHandler.this.modifyNode(branchKey().child(key), args, addition);
            }
        };
    }
}
