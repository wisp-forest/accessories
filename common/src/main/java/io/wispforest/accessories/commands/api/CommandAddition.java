package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.Objects;

public interface CommandAddition {
    ArgumentBuilder<CommandSourceStack, ?> addToBuilder(ArgumentBuilder<CommandSourceStack, ?> builder);

    default CommandAddition andWith(CommandAddition addition) {
        Objects.requireNonNull(addition);
        return (builder) -> addition.addToBuilder(addToBuilder(builder));
    }
}
