package io.wispforest.accessories.commands.api.core;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface NamedArgumentGetter<S, T> {
    T getArgument(CommandContext<S> ctx, String name) throws CommandSyntaxException;
}
