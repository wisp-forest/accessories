package io.wispforest.accessories.commands.api.core;

import io.wispforest.accessories.commands.api.base.Argument;

import java.util.ArrayList;
import java.util.List;

public interface Branch {
    Key branchKey();

    default List<Argument<?>> addStartingToArgs(List<Argument<?>> startingArgs) {
        var list = new ArrayList<>(startingArgs);

        list.addAll(0, branchKey().asArgumentList());

        return list;
    }
}
