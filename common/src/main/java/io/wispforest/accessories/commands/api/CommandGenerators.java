package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.wispforest.accessories.commands.api.base.BaseCommandGenerator;
import io.wispforest.accessories.commands.api.base.BranchedCommandGenerator;
import io.wispforest.accessories.commands.api.base.CommandGenerator;
import io.wispforest.accessories.commands.api.core.Key;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.ArrayList;
import java.util.List;

public class CommandGenerators {

    private static final List<Holder<?>> ALL_COMMAND_GENERATORS = new ArrayList<>();

    public static void create(CommandTreeGenerator<CommandSourceStack, CommandBuildContext, CommandGenerator> generateTrees) {
        create(generateTrees, argumentRegistration -> {});
    }

    public static void create(CommandTreeGenerator<CommandSourceStack, CommandBuildContext, CommandGenerator> generateTrees, OnArgumentRegistration registration) {
        ALL_COMMAND_GENERATORS.add(new Holder<>(new CommandGenerator(), generateTrees, registration));
    }

    public static void create(String key, CommandTreeGenerator<CommandSourceStack, CommandBuildContext, BranchedCommandGenerator> generateTrees) {
        create(key, generateTrees, argumentRegistration -> {});
    }

    public static void create(String key, CommandTreeGenerator<CommandSourceStack, CommandBuildContext, BranchedCommandGenerator> generateTrees, OnArgumentRegistration registrationConsumer) {
        ALL_COMMAND_GENERATORS.add(new Holder<>(new BranchedCommandGenerator(new Key(key)), generateTrees, registrationConsumer));
    }

    //--

    public static void registerAllGenerators(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection environment) {
        for (var holder : ALL_COMMAND_GENERATORS) {
            holder.registerCommands(dispatcher, context, environment);
        }
    }

    public static void registerAllArgumentTypes(ArgumentRegistrationCallback registration) {
        for (var holder : ALL_COMMAND_GENERATORS) {
            holder.registration().registerArgumentTypes(registration);
        }
    }

    private record Holder<G extends BaseCommandGenerator<CommandSourceStack, G>>(G generator, CommandTreeGenerator<CommandSourceStack, CommandBuildContext, G> treeGenerator, OnArgumentRegistration registration){
        public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection environment) {
            treeGenerator.generateTrees(generator, context, environment);

            generator.addToCommandsAndClear((string, builtRootNode) -> {
                if (!(builtRootNode instanceof LiteralArgumentBuilder<?> literalArgumentBuilder)) {
                    throw new IllegalArgumentException("A root command node was found not to be a valid root literal!!!!");
                }

                dispatcher.register((LiteralArgumentBuilder<CommandSourceStack>) literalArgumentBuilder);
            });
        }
    }
}
