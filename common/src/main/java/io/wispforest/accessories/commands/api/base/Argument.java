package io.wispforest.accessories.commands.api.base;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.wispforest.accessories.commands.api.core.ContextAwareLiteralArgumentBuilder;
import io.wispforest.accessories.commands.api.core.NamedArgumentGetter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public sealed abstract class Argument<T> permits Argument.ArgumentBuilderConstructor {

    public static <S, T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter) {
        return required(name, type, getter, null);
    }

    public static <S, T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter, T defaultValue) {
        return defaulted(name, type, getter, defaultValue, null);
    }

    public static <S, T> Argument<T> required(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter, @Nullable SuggestionProvider<?> suggestions) {
        return ArgumentWithType.of(name, type, getter, suggestions);
    }

    public static <S, T> Argument<T> defaulted(String name, ArgumentType<?> type, NamedArgumentGetter<S, T> getter, T defaultValue, @Nullable SuggestionProvider<?> suggestions) {
        return ArgumentWithType.ofDefaulted(name, type, getter, defaultValue, suggestions);
    }

    public static Argument<String> branches(String ...branches) {
        return branches(List.of(branches));
    }

    public static Argument<String> branches(List<String> branches) {
        return branches(branches, Function.identity());
    }

    public static <T> Argument<T> branches(List<String> branches, Function<String, T> conversionFunc) {
        return new LiteralBranches<>(branches, conversionFunc);
    }

    public static Argument<String> asKeyPath(String branch) {
        return LiteralBranch.asKeyPath(branch, Function.identity());
    }

    public abstract <S> T getArgument(CommandContext<S> ctx) throws CommandSyntaxException;

    // -- INTERNAL API BELOW... KINDA CRING ALSO --

    static sealed abstract class ArgumentBuilderConstructor<T> extends Argument<T> {
        public abstract <S> ArgumentBuilder<S, ?> createNodeBuilder();

        public abstract String name();

        public boolean defaulted() {
            return false;
        }
    }

    static final class ArgumentWithType<T> extends ArgumentBuilderConstructor<T> {
        private final String name;
        private final ArgumentType<?> type;
        private final NamedArgumentGetter<?, T> getter;
        private final boolean defaulted;
        private final @Nullable T defaultValue;
        @Nullable
        private final SuggestionProvider<?> suggestions;

        ArgumentWithType(String name, ArgumentType<?> type, NamedArgumentGetter<?, T> getter, boolean defaulted, @Nullable T defaultValue, @Nullable SuggestionProvider<?> suggestions) {
            this.name = name;
            this.type = type;
            this.getter = getter;
            this.defaulted = defaulted;
            this.defaultValue = defaultValue;
            this.suggestions = suggestions;
        }

        public static <T> ArgumentWithType<T> of(String name, ArgumentType<?> type, NamedArgumentGetter<?, T> getter, @Nullable SuggestionProvider<?> suggestions) {
            return new ArgumentWithType<>(name, type, getter, false, null, suggestions);
        }

        public static <T> ArgumentWithType<T> ofDefaulted(String name, ArgumentType<?> type, NamedArgumentGetter<?, T> getter, T defaultValue, @Nullable SuggestionProvider<?> suggestions) {
            return new ArgumentWithType<>(name, type, getter, true, defaultValue, suggestions);
        }

        @Override
        public <S> ArgumentBuilder<S, ?> createNodeBuilder() {
            return RequiredArgumentBuilder.argument(name, type);
        }

        @Nullable
        public <S> SuggestionProvider<S> suggestions() {
            return (SuggestionProvider<S>) suggestions;
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

    static final class LiteralBranch<T> extends ArgumentBuilderConstructor<T> {
        private final String branch;
        private final Function<String, ArgumentBuilder<?, ?>> argumentBuilderFunction;
        private final Function<String, T> conversionFunc;

        LiteralBranch(String branch, Function<String, ArgumentBuilder<?, ?>> argumentBuilderFunction, Function<String, T> conversionFunc) {
            this.branch = branch;
            this.argumentBuilderFunction = argumentBuilderFunction;
            this.conversionFunc = conversionFunc;
        }

        public static <T> LiteralBranch<T> asArgument(String branch, Function<String, T> conversionFunc) {
            return new LiteralBranch<>(branch, ContextAwareLiteralArgumentBuilder::literal, conversionFunc);
        }

        public static <T> LiteralBranch<T> asKeyPath(String branch, Function<String, T> conversionFunc) {
            return new LiteralBranch<>(branch, LiteralArgumentBuilder::literal, conversionFunc);
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
        public <S> T getArgument(CommandContext<S> ctx) throws CommandSyntaxException {
            return this.conversionFunc.apply(ContextAwareLiteralArgumentBuilder.getBranch(ctx));
        }
    }

    static final class LiteralBranches<T> extends ArgumentBuilderConstructor<T> implements ArgumentBuilderConstructorList {
        private final List<String> branches;
        private final Function<String, T> conversionFunc;

        LiteralBranches(List<String> branches, Function<String, T> conversionFunc) {
            this.branches = branches;
            this.conversionFunc = conversionFunc;
        }

        @Override
        public List<? extends ArgumentBuilderConstructor<T>> builders() {
            return branches.stream().map(str -> LiteralBranch.asArgument(str, conversionFunc)).toList();
        }

        @Override
        public <S> T getArgument(CommandContext<S> ctx) throws CommandSyntaxException {
            return conversionFunc.apply(ContextAwareLiteralArgumentBuilder.getBranch(ctx));
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
