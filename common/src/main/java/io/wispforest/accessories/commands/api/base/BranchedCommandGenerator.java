package io.wispforest.accessories.commands.api.base;

import io.wispforest.accessories.commands.api.core.Branch;
import io.wispforest.accessories.commands.api.core.CommandAddition;
import io.wispforest.accessories.commands.api.core.Key;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public final class BranchedCommandGenerator extends BaseCommandGenerator<CommandSourceStack, BranchedCommandGenerator> implements CommandTreeBuilder.BranchedCommandTreeBuilder<CommandSourceStack, BranchedCommandGenerator> {

    private final Key branchKey;

    public BranchedCommandGenerator(Key branchKey) {
        this.branchKey = branchKey;
    }

    public void modifyRootNode(CommandAddition<CommandSourceStack> addition) {
        modifyNode(this.branchKey(), addition);
    }

    public BranchedCommandNodeHandler<CommandSourceStack> modifyUnderRoot() {
        return this.modifyUnder(this.branchKey());
    }

    @Override
    public BranchedCommandGenerator createLeaves(Key key, List<Argument<?>> commandArgs, CommandAddition<CommandSourceStack> commandAddition) {
        super.createLeaves(branchKey().child(key), commandArgs, commandAddition);

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
