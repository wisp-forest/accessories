package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public final class BranchedCommandGenerator extends BaseCommandGenerator<CommandSourceStack, BranchedCommandGenerator> implements CommandTreeBuilder.BranchedCommandTreeBuilder<CommandSourceStack, BranchedCommandGenerator> {

    private final Key branchKey;

    public BranchedCommandGenerator(Key branchKey) {
        if (branchKey.isEmpty()) {
            throw new IllegalStateException("Branched Command Generators are not designed to have an empty key, use CommandGenerators.create without a key or give a valid key!");
        }

        this.branchKey = branchKey;
    }

    public void modifyRootNode(CommandAddition<CommandSourceStack> addition) {
        modifyNode(this.branchKey(), addition);
    }

    public BranchedCommandNodeHandler<CommandSourceStack> modifyUnderRoot() {
        return this.modifyUnder(this.branchKey());
    }

    @Override
    public BranchedCommandGenerator leaves(List<Argument<?>> startingArgs, List<Argument<?>> commandArgs, CommandAddition<CommandSourceStack> commandAddition) {
        super.leaves(addStartingToArgs(startingArgs), commandArgs, commandAddition);

        return getThis();
    }

    @Override
    public BranchedCommandGenerator getThis() {
        return this;
    }

    @Override
    public Key branchKey() {
        return this.branchKey;
    }
}
