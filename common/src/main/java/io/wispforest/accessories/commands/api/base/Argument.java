package io.wispforest.accessories.commands.api.base;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.accessories.commands.api.core.NamedArgumentGetter;
import io.wispforest.accessories.commands.api.core.ContextAwareLiteralArgumentBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public sealed abstract class Argument<T> permits Argument.ArgumentBuilderConstructor {

    public static <S, T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter) {
        return ArgumentWithType.of(name, type, getter);
    }

    public static <S, T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter, T defaultValue) {
        return ArgumentWithType.ofDefaulted(name, type, getter, defaultValue);
    }

    public static Argument<String> branches(String ...branches) {
        return branches(List.of(branches));
    }

    public static Argument<String> branches(List<String> branches) {
        return new LiteralBranches(branches);
    }

    public static Argument<String> asKeyPath(String branch) {
        return LiteralBranch.asKeyPath(branch);
    }

    public abstract <S> T getArgument(CommandContext<S> ctx) throws CommandSyntaxException;

    // -- INTERNAL API BELOW... KINDA CRING ALSO --

    static non-sealed abstract class ArgumentBuilderConstructor<T> extends Argument<T> {
        public abstract <S> ArgumentBuilder<S, ?> createNodeBuilder();

        public abstract String name();

        public boolean defaulted() {
            return false;
        }
    }

    static class ArgumentWithType<T> extends ArgumentBuilderConstructor<T> {
        private final String name;
        private final ArgumentType<?> type;
        private final NamedArgumentGetter<?, T> getter;
        private final boolean defaulted;
        private final @Nullable T defaultValue;

        ArgumentWithType(String name, ArgumentType<?> type, NamedArgumentGetter<?, T> getter, boolean defaulted, @Nullable T defaultValue) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.defaulted = defaulted;
            this.defaultValue = defaultValue;
        }

        public static <T> ArgumentWithType<T> of(String name, ArgumentType<?> type, NamedArgumentGetter<?, T> getter) {
            return new ArgumentWithType<>(name, type, getter, false, null);
        }

        public static <T> ArgumentWithType<T> ofDefaulted(String name, ArgumentType<?> type, NamedArgumentGetter<?, T> getter, T defaultValue) {
            return new ArgumentWithType<>(name, type, getter, true, defaultValue);
        }

        @Override
        public <S> ArgumentBuilder<S, ?> createNodeBuilder() {
            return RequiredArgumentBuilder.argument(name, type);
        }

        @Override
        public <S> T getArgument(CommandContext<S> ctx) throws CommandSyntaxException {
            try {
                return ((NamedArgumentGetter<S, T>) this.getter).getArgument(ctx, name);
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
        private final Function<String, ArgumentBuilder<?, ?>> argumentBuilderFunction;

        LiteralBranch(String branch, Function<String, ArgumentBuilder<?, ?>> argumentBuilderFunction) {
            this.branch = branch;
            this.argumentBuilderFunction = argumentBuilderFunction;
        }

        public static LiteralBranch asArgument(String branch) {
            return new LiteralBranch(branch, ContextAwareLiteralArgumentBuilder::literal);
        }

        public static LiteralBranch asKeyPath(String branch) {
            return new LiteralBranch(branch, LiteralArgumentBuilder::literal);
        }

        @Override
        public <S> ArgumentBuilder<S, ?> createNodeBuilder() {
            return (ArgumentBuilder<S, ?>) argumentBuilderFunction.apply(branch);
        }

        @Override
        public String name() {
            return branch;
        }

        @Override
        public <S> String getArgument(CommandContext<S> ctx) throws CommandSyntaxException {
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
        public <S> String getArgument(CommandContext<S> ctx) throws CommandSyntaxException {
            return ContextAwareLiteralArgumentBuilder.getBranch(ctx);
        }

        @Override
        public <S> ArgumentBuilder<S, ?> createNodeBuilder() {
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
