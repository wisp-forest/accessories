package io.wispforest.accessories.commands.api.base;

import net.minecraft.commands.CommandSourceStack;

public final class CommandGenerator extends BaseCommandGenerator<CommandSourceStack, CommandGenerator> {
    @Override
    public CommandGenerator getThis() {
        return this;
    }
}
