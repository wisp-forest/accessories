package io.wispforest.accessories.commands.api;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public sealed abstract class Argument<T> permits Argument.ArgumentBuilderConstructor {

    static <T> Argument<T> arg(String name, ArgumentType<?> type, CommandBuilderHelper.NamedArgumentGetter<T> getter) {
        return ArgumentWithType.of(name, type, getter);
    }

    static <T> Argument<T> defaultedArg(String name, ArgumentType<?> type, CommandBuilderHelper.NamedArgumentGetter<T> getter, T defaultValue) {
        return ArgumentWithType.defaulted(name, type, getter, defaultValue);
    }

    static Argument<String> branches(String ...branches) {
        return branches(List.of(branches));
    }

    static Argument<String> branches(List<String> branches) {
        return new LiteralBranches(branches);
    }

    public abstract T getArgument(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;

    static non-sealed abstract class ArgumentBuilderConstructor<T> extends Argument<T> {
        public abstract ArgumentBuilder<CommandSourceStack, ?> createNodeBuilder();

        public abstract String name();

        public boolean defaulted() {
            return false;
        }
    }

    static class ArgumentWithType<T> extends ArgumentBuilderConstructor<T> {
        private final String name;
        private final ArgumentType<?> type;
        private final CommandBuilderHelper.NamedArgumentGetter<T> getter;
        private final boolean defaulted;
        private final @Nullable T defaultValue;

        ArgumentWithType(String name, ArgumentType<?> type, CommandBuilderHelper.NamedArgumentGetter<T> getter, boolean defaulted, @Nullable T defaultValue) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.defaulted = defaulted;
            this.defaultValue = defaultValue;
        }

        public static <T> ArgumentWithType<T> of(String name, ArgumentType<?> type, CommandBuilderHelper.NamedArgumentGetter<T> getter) {
            return new ArgumentWithType<>(name, type, getter, false, null);
        }

        public static <T> ArgumentWithType<T> defaulted(String name, ArgumentType<?> type, CommandBuilderHelper.NamedArgumentGetter<T> getter, T defaultValue) {
            return new ArgumentWithType<>(name, type, getter, true, defaultValue);
        }

        @Override
        public ArgumentBuilder<CommandSourceStack, ?> createNodeBuilder() {
            return Commands.argument(name, type);
        }

        @Override
        public T getArgument(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            try {
                return this.getter.getArgument(ctx, name);
            } catch (IllegalArgumentException e) {
                if (defaulted) return defaultValue;

                throw e;
            }
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean defaulted() {
            return defaulted;
        }
    }

    static final class LiteralBranch extends ArgumentBuilderConstructor<String> {
        private final String branch;
        private final Function<String, ArgumentBuilder<CommandSourceStack, ?>> argumentBuilderFunction;

        LiteralBranch(String branch, Function<String, ArgumentBuilder<CommandSourceStack, ?>> argumentBuilderFunction) {
            this.branch = branch;
            this.argumentBuilderFunction = argumentBuilderFunction;
        }

        public static LiteralBranch asArgument(String branch) {
            return new LiteralBranch(branch, ContextAwareLiteralArgumentBuilder::literal);
        }

        public static LiteralBranch of(String branch) {
            return new LiteralBranch(branch, LiteralArgumentBuilder::literal);
        }

        @Override
        public ArgumentBuilder<CommandSourceStack, ?> createNodeBuilder() {
            return argumentBuilderFunction.apply(branch);
        }

        @Override
        public String name() {
            return branch;
        }

        @Override
        public String getArgument(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            return ContextAwareLiteralArgumentBuilder.getBranch(ctx);
        }
    }

    static final class LiteralBranches extends ArgumentBuilderConstructor<String> implements ArgumentBuilderConstructorList {
        private final List<String> branches;

        LiteralBranches(List<String> branches) {
            this.branches = branches;
        }

        @Override
        public List<? extends ArgumentBuilderConstructor> builders() {
            return branches.stream().map(LiteralBranch::asArgument).toList();
        }

        @Override
        public String getArgument(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            return ContextAwareLiteralArgumentBuilder.getBranch(ctx);
        }

        @Override
        public ArgumentBuilder<CommandSourceStack, ?> createNodeBuilder() {
            throw new IllegalArgumentException("Unable to create node builder for LiteralBranches");
        }

        @Override
        public String name() {
            throw new IllegalArgumentException("Unable to get name for LiteralBranches");
        }
    }

    interface ArgumentBuilderConstructorList {
        List<? extends ArgumentBuilderConstructor> builders();
    }
}
