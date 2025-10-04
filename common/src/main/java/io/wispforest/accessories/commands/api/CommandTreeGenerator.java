package io.wispforest.accessories.commands.api;

import io.wispforest.accessories.commands.api.base.BaseCommandGenerator;
import io.wispforest.accessories.commands.api.base.BranchedCommandGenerator;
import io.wispforest.accessories.commands.api.base.CommandGenerator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public interface CommandTreeGenerator<S, B, G extends BaseCommandGenerator<S, G>> extends ArgumentsWithContext<S> {
    void generateTrees(G rootGenerator, B context, Commands.CommandSelection environment);

    interface Branched extends CommandTreeGenerator<CommandSourceStack, CommandBuildContext, BranchedCommandGenerator> { }

    interface Base extends CommandTreeGenerator<CommandSourceStack, CommandBuildContext, CommandGenerator> { }
}
